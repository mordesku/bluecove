/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2006-2007 Vlad Skarzhevskyy
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 *  @version $Id$
 */

#include "common.h"

#include "com_intel_bluetooth_BluetoothPeer.h"

JNIEXPORT jbyteArray JNICALL Java_com_intel_bluetooth_BluetoothPeer_testUUIDConversion
(JNIEnv *env, jclass, jbyteArray uuidValue) {
	GUID service_guid;
	// pin array
	jbyte *bytes = env->GetByteArrayElements(uuidValue, 0);
	// build UUID
	convertUUIDBytesToGUID(bytes, &service_guid);
	// unpin array
	env->ReleaseByteArrayElements(uuidValue, bytes, 0);

	jbyteArray uuidValueConverted = env->NewByteArray(16);
	jbyte *bytesConverted = env->GetByteArrayElements(uuidValueConverted, 0);

	convertGUIDToUUIDBytes(&service_guid, bytesConverted);

	env->ReleaseByteArrayElements(uuidValueConverted, bytesConverted, 0);

	return uuidValueConverted;
}

JNIEXPORT jlong JNICALL Java_com_intel_bluetooth_BluetoothPeer_testReceiveBufferCreate
(JNIEnv *, jclass, jint size) {
	return (jlong) new ReceiveBuffer(size);
}

JNIEXPORT void JNICALL Java_com_intel_bluetooth_BluetoothPeer_testReceiveBufferClose
(JNIEnv *, jclass, jlong bufferHandler) {
	ReceiveBuffer* b = (ReceiveBuffer*)bufferHandler;
	delete b;
}

JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothPeer_testReceiveBufferWrite
(JNIEnv *env, jclass, jlong bufferHandler, jbyteArray data) {
	ReceiveBuffer* b = (ReceiveBuffer*)bufferHandler;
	jbyte *bytes = env->GetByteArrayElements(data, 0);
	jint rc = b->write(bytes, env->GetArrayLength(data));
	env->ReleaseByteArrayElements(data, bytes, 0);
	return rc;
}

JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothPeer_testReceiveBufferRead__J_3B
(JNIEnv *env, jclass, jlong bufferHandler, jbyteArray data) {
	ReceiveBuffer* b = (ReceiveBuffer*)bufferHandler;
	jbyte *bytes = env->GetByteArrayElements(data, 0);
	jint rc = b->read(bytes, env->GetArrayLength(data));
	env->ReleaseByteArrayElements(data, bytes, 0);
	return rc;
}

JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothPeer_testReceiveBufferRead__J
(JNIEnv *env, jclass, jlong bufferHandler) {
	ReceiveBuffer* b = (ReceiveBuffer*)bufferHandler;
	return b->readByte();
}

JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothPeer_testReceiveBufferAvailable
(JNIEnv *env, jclass, jlong bufferHandler) {
	ReceiveBuffer* b = (ReceiveBuffer*)bufferHandler;
	return b->available();
}

JNIEXPORT jboolean JNICALL Java_com_intel_bluetooth_BluetoothPeer_testReceiveBufferIsOverflown
(JNIEnv *, jclass, jlong bufferHandler) {
	ReceiveBuffer* b = (ReceiveBuffer*)bufferHandler;
	return b->isOverflown();
}

JNIEXPORT jboolean JNICALL Java_com_intel_bluetooth_BluetoothPeer_testReceiveBufferIsCorrupted
(JNIEnv *, jclass, jlong bufferHandler) {
	ReceiveBuffer* b = (ReceiveBuffer*)bufferHandler;
	return b->isCorrupted();
}