/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.tools.idea.templates

import com.android.SdkConstants.ATTR_ID
import com.android.SdkConstants.ATTR_NAME
import com.android.SdkConstants.FN_ANDROID_MANIFEST_XML
import com.android.SdkConstants.XMLNS_PREFIX
import com.android.manifmerger.ManifestMerger2
import com.android.manifmerger.MergingReport
import com.android.manifmerger.XmlDocument
import com.android.resources.ResourceFolderType
import com.android.tools.idea.templates.recipe.RenderingContext
import com.android.utils.StdLogger
import com.android.utils.XmlUtils
import com.google.common.base.Charsets
import com.google.common.base.Splitter
import com.google.common.collect.Lists.newArrayList
import com.google.common.collect.Maps
import com.intellij.lang.xml.XMLLanguage
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.xml.XmlComment
import com.intellij.psi.xml.XmlFile
import com.intellij.psi.xml.XmlTag
import com.intellij.psi.xml.XmlTagChild
import com.intellij.psi.xml.XmlText
import com.intellij.util.SystemProperties
import org.apache.commons.lang.StringUtils
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileNotFoundException
import java.io.InputStream
import java.util.LinkedList

/**
 * Utility class to support the recipe.xml merge instruction.
 */
object RecipeMergeUtils {
  private val LOG = Logger.getInstance(RecipeMergeUtils::class.java)

  private const val MERGE_ATTR_STRATEGY = "templateMergeStrategy"
  private const val MERGE_ATTR_STRATEGY_REPLACE = "replace"
  private const val MERGE_ATTR_STRATEGY_PRESERVE = "preserve"

  @Throws(RuntimeException::class)
  fun mergeGradleSettingsFile(source: String, dest: String): String {
    // TODO: Right now this is implemented as a dumb text merge. It would be much better to read it into PSI using IJ's Groovy support.
    // If Gradle build files get first-class PSI support in the future, we will pick that up cheaply. At the moment, Our Gradle-Groovy
    // support requires a project, which we don't necessarily have when instantiating a template.

    /*
    Add new include lines instead of merging everything in a single line (b/133578918)

    For simplicity, this will add new lines at the end of the file. Trying to look for "include" lines could cause issues if we do not
    consider all cases, for example if there are comment blocks or 'include' directives inside functions. See this for some examples:

    https://docs.gradle.org/current/dsl/org.gradle.api.initialization.Settings.html#org.gradle.api.initialization.Settings:include(java.lang.String[])
     */

    val includeLines = LinkedList<String>()
    for (line in Splitter.on('\n').omitEmptyStrings().trimResults().split(source)) {
      if (!line.startsWith("include")) {
        throw RuntimeException("When merging settings.gradle files, only include directives can be merged.")
      }
      includeLines.add(line)
    }
    if (includeLines.isEmpty()) {
      return dest
    }

    val contents = StringBuilder(StringUtils.stripEnd(StringUtils.chomp(dest), null))
    val lineSeparator = SystemProperties.getLineSeparator()
    if (contents.isNotEmpty()) {
      contents.append(lineSeparator)
    }
    contents.append(includeLines.joinToString(lineSeparator))
    contents.append(lineSeparator)

    return contents.toString()
  }

  /**
   * Merges sourceXml into targetXml/targetFile (targetXml is the contents of targetFile).
   * Returns the resulting xml if it still needs to be written to targetFile,
   * or null if the file has already been/doesn't need to be updated.
   */
  fun mergeXml(context: RenderingContext, sourceXml: String, targetXml: String, targetFile: File): String {
    val ok: Boolean
    val fileName = targetFile.name
    var contents: String?
    var errors: String? = null
    if (fileName == FN_ANDROID_MANIFEST_XML) {
      val currentDocument = XmlUtils.parseDocumentSilently(targetXml, true) ?: error("$targetXml failed to parse")
      val fragment = XmlUtils.parseDocumentSilently(sourceXml, true) ?: error("$sourceXml failed to parse")
      val report = mergeManifest(context.moduleRoot, targetFile, targetXml, sourceXml)
      if (report != null && report.result.isSuccess) {
        contents = report.getMergedDocument(MergingReport.MergedManifestKind.MERGED)
      }
      else {
        contents = null
        if (report != null) {
          // report.getReportString() isn't useful, it just says to look at the logs
          val sb = StringBuilder()
          for (record in report.loggingRecords) {
            val severity = record.severity
            if (severity != MergingReport.Record.Severity.ERROR) {
              // Some of the warnings are misleading -- e.g. "missing package declaration";
              // that's deliberate. Users only have to deal with errors to get the
              // manifest merge to succeed.
              continue
            }
            sb.append("* ")
            sb.append(record.message)
            sb.append("\n\n")
          }

          errors = sb.toString()
          // Error messages may refer to our internal temp name for the target manifest file

          errors = errors.replace("AndroidManifest.xml", "current AndroidManifest.xml")

          errors = errors.replace("nevercreated.xml", "template AndroidManifest.xml")
          errors = errors.trim { it <= ' ' }
        }
      }

      ok = contents != null
    }
    else {
      // Merge plain XML files
      val parentFolderName = targetFile.parentFile.name
      val folderType = ResourceFolderType.getFolderType(parentFolderName)
      // mergeResourceFile handles the file updates itself, so no content is returned in this case.
      contents = mergeResourceFile(context, targetXml, sourceXml, fileName, folderType)
      ok = true
    }

    // Finally write out the merged file
    if (!ok) {
      // Just insert into file along with comment, using the "standard" conflict
      // syntax that many tools and editors recognize.

      contents = wrapWithMergeConflict(targetXml, sourceXml)

      // Report the conflict as a warning:
      context.warnings.add(String.format(
        "Merge conflict for: %1\$s\nThis file must be fixed by hand. The errors " + "encountered during the merge are:\n\n%2\$s",
        targetFile.name, errors))
    }
    return contents!!
  }

  /**
   * Merges the given resource file contents into the given resource file
   */
  fun mergeResourceFile(context: RenderingContext,
                        targetXml: String,
                        sourceXml: String,
                        fileName: String,
                        folderType: ResourceFolderType?): String {
    val targetPsiFile = PsiFileFactory.getInstance(context.project)
      .createFileFromText("targetFile", XMLLanguage.INSTANCE, StringUtil.convertLineSeparators(targetXml), false, true) as XmlFile
    val sourcePsiFile = PsiFileFactory.getInstance(context.project)
      .createFileFromText("sourceFile", XMLLanguage.INSTANCE, StringUtil.convertLineSeparators(sourceXml), false, true) as XmlFile
    val root = targetPsiFile.document!!.rootTag ?: error("Cannot find XML root in target: $targetXml")

    val attributes = sourcePsiFile.rootTag!!.attributes
    for (attr in attributes) {
      if (attr.namespacePrefix == XMLNS_PREFIX) {
        root.setAttribute(attr.name, attr.value)
      }
    }

    val prependElements = newArrayList<XmlTagChild>()
    var indent: XmlText? = null
    if (folderType == ResourceFolderType.VALUES) {
      // Try to merge items of the same name
      val old = Maps.newHashMap<String, XmlTag>()
      for (newSibling in root.subTags) {
        old[getResourceId(newSibling)] = newSibling
      }
      for (child in sourcePsiFile.rootTag!!.children) {
        when (child) {
          is XmlComment -> {
            if (indent != null) {
              prependElements.add(indent)
            }
            prependElements.add(child as XmlTagChild)
          }
          is XmlText -> indent = child
          is XmlTag -> {
            var subTag = child
            val mergeStrategy = subTag.getAttributeValue(MERGE_ATTR_STRATEGY)
            subTag.setAttribute(MERGE_ATTR_STRATEGY, null)
            // remove the space left by the deleted attribute
            CodeStyleManager.getInstance(context.project).reformat(subTag)
            val name = getResourceId(subTag)
            val replace = if (name != null) old[name] else null
            if (replace != null) {
              // There is an existing item with the same id. Either replace it
              // or preserve it depending on the "templateMergeStrategy" attribute.
              // If that attribute does not exist, default to preserving it.

              // Let's say you've used the activity wizard once, and it
              // emits some configuration parameter as a resource that
              // it depends on, say "padding". Then the user goes and
              // tweaks the padding to some other number.
              // Now running the wizard a *second* time for some new activity,
              // we should NOT go and set the value back to the template's
              // default!
              when {
                MERGE_ATTR_STRATEGY_REPLACE == mergeStrategy -> {
                  val newChild = replace.replace(child)
                  // When we're replacing, the line is probably already indented. Skip the initial indent
                  if (newChild.prevSibling is XmlText && prependElements[0] is XmlText) {
                    prependElements.removeAt(0)
                    // If we're adding something we'll need a newline/indent after it
                    if (prependElements.isNotEmpty()) {
                      prependElements.add(indent)
                    }
                  }
                  for (element in prependElements) {
                    root.addBefore(element, newChild)
                  }
                }
                MERGE_ATTR_STRATEGY_PRESERVE == mergeStrategy -> {
                  // Preserve the existing value.
                }
                replace.text.trim { it <= ' ' } == child.getText().trim { it <= ' ' } -> {
                  // There are no differences, do not issue a warning.
                }
                else -> // No explicit directive given, preserve the original value by default.
                  context.warnings.add(String.format(
                    "Ignoring conflict for the value: %1\$s wanted: \"%2\$s\" but it already is: \"%3\$s\" in the file: %4\$s", name,
                    child.getText(), replace.text, fileName))
              }
            }
            else {
              if (indent != null) {
                prependElements.add(indent)
              }
              subTag = root.addSubTag(subTag, false)
              for (element in prependElements) {
                root.addBefore(element, subTag)
              }
            }
            prependElements.clear()
          }
        }
      }
    }
    else {
      // In other file types, such as layouts, just append all the new content
      // at the end.
      for (child in sourcePsiFile.rootTag!!.children) {
        if (child is XmlTag) {
          root.addSubTag(child, false)
        }
      }
    }
    return targetPsiFile.text
  }

  /**
   * Merges the given manifest fragment into the given manifest file
   */
  private fun mergeManifest(moduleRoot: File, targetManifest: File,
                            targetXml: String, mergeText: String): MergingReport? {
    try {
      val isMasterManifest = FileUtil.filesEqual(moduleRoot, targetManifest.parentFile)

      val tempFile2 = File(targetManifest.parentFile, "nevercreated.xml")
      val logger = StdLogger(StdLogger.Level.INFO)
      return ManifestMerger2.newMerger(targetManifest, logger, ManifestMerger2.MergeType.APPLICATION)
        .withFeatures(ManifestMerger2.Invoker.Feature.EXTRACT_FQCNS,
                      ManifestMerger2.Invoker.Feature.HANDLE_VALUE_CONFLICTS_AUTOMATICALLY,
                      ManifestMerger2.Invoker.Feature.NO_PLACEHOLDER_REPLACEMENT)
        .addFlavorAndBuildTypeManifest(tempFile2)
        .asType(if (isMasterManifest) XmlDocument.Type.MAIN else XmlDocument.Type.OVERLAY)
        .withFileStreamProvider(object : ManifestMerger2.FileStreamProvider() {
          @Throws(FileNotFoundException::class)
          override fun getInputStream(file: File): InputStream {
            val text = if (FileUtil.filesEqual(file, targetManifest)) targetXml else mergeText
            return ByteArrayInputStream(text.toByteArray(Charsets.UTF_8))
          }
        })
        .merge()
    }
    catch (e: ManifestMerger2.MergeFailureException) {
      LOG.warn(e)
      return null
    }
  }

  private fun getResourceId(tag: XmlTag): String? {
    var name = tag.getAttributeValue(ATTR_NAME)
    if (name == null) {
      name = tag.getAttributeValue(ATTR_ID)
    }
    return name
  }

  /**
   * Wraps the given strings in the standard conflict syntax
   */
  private fun wrapWithMergeConflict(original: String, added: String): String {
    val sep = "\n"
    return "<<<<<<< Original$sep$original$sep=======$sep$added>>>>>>> Added$sep"
  }
}
