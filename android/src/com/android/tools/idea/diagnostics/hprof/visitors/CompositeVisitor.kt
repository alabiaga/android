/*
 * Copyright (C) 2018 The Android Open Source Project
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
package com.android.tools.idea.diagnostics.hprof.visitors

import com.android.tools.idea.diagnostics.hprof.parser.ConstantPoolEntry
import com.android.tools.idea.diagnostics.hprof.parser.HProfVisitor
import com.android.tools.idea.diagnostics.hprof.parser.HeapDumpRecordType
import com.android.tools.idea.diagnostics.hprof.parser.InstanceFieldEntry
import com.android.tools.idea.diagnostics.hprof.parser.RecordType
import com.android.tools.idea.diagnostics.hprof.parser.StaticFieldEntry
import com.android.tools.idea.diagnostics.hprof.parser.Type
import java.nio.ByteBuffer

class CompositeVisitor(private vararg val visitors: HProfVisitor) : HProfVisitor() {

  override fun preVisit() {
    visitors.forEach {
      it.visitorContext = visitorContext
    }

    visitors.forEach { it.preVisit() }
    disableAll()
    RecordType.values().forEach { type ->
      if (visitors.any { it.isEnabled(type) })
        enable(type)
      else
        disable(type)
    }
    HeapDumpRecordType.values().forEach { type ->
      if (visitors.any { it.isEnabled(type) })
        enable(type)
      else
        disable(type)
    }
  }

  override fun postVisit() {
    visitors.forEach { it.postVisit() }
  }

  override fun visitStringInUTF8(id: Long, s: String) {
    visitors.forEach { it.visitStringInUTF8(id, s) }
  }

  override fun visitLoadClass(classSerialNumber: Long, classObjectId: Long, stackSerialNumber: Long, classNameStringId: Long) {
    visitors.forEach { it.visitLoadClass(classSerialNumber, classObjectId, stackSerialNumber, classNameStringId) }
  }

  override fun visitStackFrame() {
    visitors.forEach { it.visitStackFrame() }
  }

  override fun visitStackTrace() {
    visitors.forEach { it.visitStackTrace() }
  }

  override fun visitAllocSites() {
    visitors.forEach { it.visitAllocSites() }
  }

  override fun visitHeapSummary(totalLiveBytes: Long, totalLiveInstances: Long, totalBytesAllocated: Long, totalInstancesAllocated: Long) {
    visitors.forEach { it.visitHeapSummary(totalLiveBytes, totalLiveInstances, totalBytesAllocated, totalInstancesAllocated) }
  }

  override fun visitStartThread() {
    visitors.forEach { it.visitStartThread() }
  }

  override fun visitEndThread(threadSerialNumber: Long) {
    visitors.forEach { it.visitEndThread(threadSerialNumber) }
  }

  override fun visitHeapDump() {
    visitors.forEach { it.visitHeapDump() }
  }

  override fun visitHeapDumpEnd() {
    visitors.forEach { it.visitHeapDumpEnd() }
  }

  override fun visitCPUSamples() {
    visitors.forEach { it.visitCPUSamples() }
  }

  override fun visitControlSettings() {
    visitors.forEach { it.visitControlSettings() }
  }

  override fun visitRootUnknown(objectId: Long) {
    visitors.forEach { it.visitRootUnknown(objectId) }
  }

  override fun visitRootGlobalJNI(objectId: Long, jniGlobalRefId: Long) {
    visitors.forEach { it.visitRootGlobalJNI(objectId, jniGlobalRefId) }
  }

  override fun visitRootLocalJNI(objectId: Long, threadSerialNumber: Long, frameNumber: Long) {
    visitors.forEach { it.visitRootLocalJNI(objectId, threadSerialNumber, frameNumber) }
  }

  override fun visitRootJavaFrame(objectId: Long, threadSerialNumber: Long, frameNumber: Long) {
    visitors.forEach { it.visitRootJavaFrame(objectId, threadSerialNumber, frameNumber) }
  }

  override fun visitRootNativeStack(objectId: Long, threadSerialNumber: Long) {
    visitors.forEach { it.visitRootNativeStack(objectId, threadSerialNumber) }
  }

  override fun visitRootStickyClass(objectId: Long) {
    visitors.forEach { it.visitRootStickyClass(objectId) }
  }

  override fun visitRootThreadBlock(objectId: Long, threadSerialNumber: Long) {
    visitors.forEach { it.visitRootThreadBlock(objectId, threadSerialNumber) }
  }

  override fun visitRootMonitorUsed(objectId: Long) {
    visitors.forEach { it.visitRootMonitorUsed(objectId) }
  }

  override fun visitRootThreadObject(objectId: Long, threadSerialNumber: Long, stackTraceSerialNumber: Long) {
    visitors.forEach { it.visitRootThreadObject(objectId, threadSerialNumber, stackTraceSerialNumber) }
  }

  override fun visitPrimitiveArrayDump(arrayObjectId: Long, stackTraceSerialNumber: Long, numberOfElements: Long, elementType: Type) {
    visitors.forEach { it.visitPrimitiveArrayDump(arrayObjectId, stackTraceSerialNumber, numberOfElements, elementType) }
  }

  override fun visitClassDump(classId: Long,
                              stackTraceSerialNumber: Long,
                              superClassId: Long,
                              classloaderClassId: Long,
                              instanceSize: Long,
                              constants: Array<ConstantPoolEntry>,
                              staticFields: Array<StaticFieldEntry>,
                              instanceFields: Array<InstanceFieldEntry>) {
    visitors.forEach {
      it.visitClassDump(classId, stackTraceSerialNumber, superClassId, classloaderClassId, instanceSize, constants, staticFields,
                        instanceFields)
    }
  }

  override fun visitObjectArrayDump(arrayObjectId: Long, stackTraceSerialNumber: Long, arrayClassObjectId: Long, objects: LongArray) {
    visitors.forEach { it.visitObjectArrayDump(arrayObjectId, stackTraceSerialNumber, arrayClassObjectId, objects) }
  }

  override fun visitInstanceDump(objectId: Long, stackTraceSerialNumber: Long, classObjectId: Long, bytes: ByteBuffer) {
    visitors.forEach { it.visitInstanceDump(objectId, stackTraceSerialNumber, classObjectId, bytes) }
  }

  override fun visitUnloadClass(classSerialNumber: Long) {
    visitors.forEach { it.visitUnloadClass(classSerialNumber) }
  }

}