/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2008 Michael Lifshits
 *  Copyright (C) 2008 Vlad Skarzhevskyy
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
package com.intel.bluetooth;

import java.io.IOException;

import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.DeviceClass;
import javax.bluetooth.DiscoveryListener;
import javax.bluetooth.RemoteDevice;
import javax.bluetooth.ServiceRegistrationException;
import javax.bluetooth.UUID;

class BluetoothEmulator implements BluetoothStack {

	static final int NATIVE_LIBRARY_VERSION = BlueCoveImpl.nativeLibraryVersionExpected;

	private EmulatorLocalDevice localDevice;

	private EmulatorDeviceInquiry deviceInquiry;

	BluetoothEmulator() {
	}

	// --- Library initialization

	public String getStackID() {
		return BlueCoveImpl.STACK_EMULATOR;
	}

	public int getLibraryVersion() throws BluetoothStateException {
		return NATIVE_LIBRARY_VERSION;
	}

	public int detectBluetoothStack() {
		return BlueCoveImpl.BLUECOVE_STACK_DETECT_EMULATOR;
	}

	public void initialize() throws BluetoothStateException {
		localDevice = EmulatorHelper.createNewLocalDevice();
	}

	public void destroy() {
		EmulatorHelper.releaseDevice(localDevice);
	}

	public void enableNativeDebug(Class nativeDebugCallback, boolean on) {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.intel.bluetooth.BluetoothStack#isCurrentThreadInterruptedCallback()
	 */
	public boolean isCurrentThreadInterruptedCallback() {
		return Thread.interrupted();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.intel.bluetooth.BluetoothStack#getFeatureSet()
	 */
	public int getFeatureSet() {
		return FEATURE_SET_DEVICE_SERVICE_CLASSES | FEATURE_SERVICE_ATTRIBUTES | FEATURE_L2CAP;
	}

	// --- LocalDevice

	public String getLocalDeviceBluetoothAddress() throws BluetoothStateException {
		return RemoteDeviceHelper.getBluetoothAddress(localDevice.getAddress());
	}

	public DeviceClass getLocalDeviceClass() {
		return new DeviceClass(localDevice.getDeviceClass());
	}

	public String getLocalDeviceName() {
		return localDevice.getName();
	}

	public boolean isLocalDevicePowerOn() {
		return localDevice.isLocalDevicePowerOn();
	}

	public String getLocalDeviceProperty(String property) {
		return localDevice.getLocalDeviceProperty(property);
	}

	public int getLocalDeviceDiscoverable() {
		return localDevice.getLocalDeviceDiscoverable();
	}

	public boolean setLocalDeviceDiscoverable(int mode) throws BluetoothStateException {
		return localDevice.setLocalDeviceDiscoverable(mode);
	}

	private EmulatorLocalDevice activeLocalDevice() throws BluetoothStateException {
		if (!localDevice.isActive()) {
			throw new BluetoothStateException("Bluetooth system is off");
		}
		return localDevice;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.intel.bluetooth.BluetoothStack#setLocalDeviceServiceClasses(int)
	 */
	public void setLocalDeviceServiceClasses(int classOfDevice) {
		localDevice.setLocalDeviceServiceClasses(classOfDevice);
	}

	// --- Remote Device authentication

	public boolean authenticateRemoteDevice(long address) throws IOException {
		return false;
	}

	// --- Device Inquiry

	public boolean startInquiry(int accessCode, DiscoveryListener listener) throws BluetoothStateException {
		if (deviceInquiry != null) {
			throw new BluetoothStateException("Another inquiry already running");
		}
		deviceInquiry = new EmulatorDeviceInquiry(activeLocalDevice(), this, listener);
		return DeviceInquiryThread.startInquiry(deviceInquiry, accessCode, listener);
	}

	public boolean cancelInquiry(DiscoveryListener listener) {
		if (deviceInquiry == null) {
			return false;
		}
		if (deviceInquiry.cancelInquiry(listener)) {
			deviceInquiry = null;
			return true;
		} else {
			return false;
		}
	}

	public String getRemoteDeviceFriendlyName(long address) throws IOException {
		return activeLocalDevice().getDeviceManagerService().getRemoteDeviceFriendlyName(address);
	}

	// --- Service search

	public int searchServices(int[] attrSet, UUID[] uuidSet, RemoteDevice device, DiscoveryListener listener)
			throws BluetoothStateException {
		return SearchServicesThread.startSearchServices(this, new EmulatorSearchServices(activeLocalDevice(), this),
				attrSet, uuidSet, device, listener);
	}

	public boolean cancelServiceSearch(int transID) {
		SearchServicesThread sst = SearchServicesThread.getServiceSearchThread(transID);
		if (sst != null) {
			synchronized (sst) {
				if (!sst.isTerminated()) {
					sst.setTerminated();
					return true;
				}
			}
		}
		return false;
	}

	public boolean populateServicesRecordAttributeValues(ServiceRecordImpl serviceRecord, int[] attrIDs)
			throws IOException {
		if (attrIDs.length > localDevice.getBluetooth_sd_attr_retrievable_max()) {
			throw new IllegalArgumentException();
		}
		return EmulatorSearchServices.populateServicesRecordAttributeValues(activeLocalDevice(), serviceRecord,
				attrIDs, RemoteDeviceHelper.getAddress(serviceRecord.getHostDevice()), serviceRecord.getHandle());
	}

	// --- Client RFCOMM connections

	public long connectionRfOpenClientConnection(BluetoothConnectionParams params) throws IOException {
		EmulatorRFCOMMClient c = activeLocalDevice().createRFCOMMClient();
		boolean success = false;
		try {
			c.connect(params);
			success = true;
		} finally {
			if (!success) {
				localDevice.removeConnection(c);
			}
		}
		return c.getHandle();
	}

	public void connectionRfCloseClientConnection(long handle) throws IOException {
		EmulatorRFCOMMClient c = ((EmulatorRFCOMMClient) localDevice.getConnection(handle));
		try {
			c.close();
		} finally {
			localDevice.removeConnection(c);
		}
	}

	public int rfGetSecurityOpt(long handle, int expected) throws IOException {
		return ((EmulatorLinkedConnection) localDevice.getConnection(handle)).getSecurityOpt(expected);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.intel.bluetooth.BluetoothStack#l2Encrypt(long,long,boolean)
	 */
	public boolean rfEncrypt(long address, long handle, boolean on) throws IOException {
		return ((EmulatorLinkedConnection) localDevice.getConnection(handle)).encrypt(address, on);
	}

	// --- Server RFCOMM connections

	public long rfServerOpen(BluetoothConnectionNotifierParams params, ServiceRecordImpl serviceRecord)
			throws IOException {
		EmulatorRFCOMMService s = activeLocalDevice().createRFCOMMService();
		boolean success = false;
		try {
			s.open(params);
			serviceRecord.setHandle(s.getHandle());
			serviceRecord
					.populateRFCOMMAttributes(s.getHandle(), s.getChannel(), params.uuid, params.name, params.obex);
			s.updateServiceRecord(serviceRecord);
			success = true;
		} finally {
			if (!success) {
				localDevice.removeConnection(s);
			}
		}
		return s.getHandle();
	}

	public void rfServerClose(long handle, ServiceRecordImpl serviceRecord) throws IOException {
		EmulatorRFCOMMService s = ((EmulatorRFCOMMService) localDevice.getConnection(handle));
		try {
			s.close(serviceRecord);
		} finally {
			localDevice.removeConnection(s);
		}
	}

	private void serverUpdateServiceRecord(long handle, ServiceRecordImpl serviceRecord, boolean acceptAndOpen)
			throws ServiceRegistrationException {
		EmulatorServiceConnection s;
		try {
			s = ((EmulatorServiceConnection) activeLocalDevice().getConnection(handle));
		} catch (IOException e) {
			throw new ServiceRegistrationException(e.getMessage());
		}
		s.updateServiceRecord(serviceRecord);
	}

	public void rfServerUpdateServiceRecord(long handle, ServiceRecordImpl serviceRecord, boolean acceptAndOpen)
			throws ServiceRegistrationException {
		serverUpdateServiceRecord(handle, serviceRecord, acceptAndOpen);
	}

	public long rfServerAcceptAndOpenRfServerConnection(long handle) throws IOException {
		EmulatorRFCOMMService s = ((EmulatorRFCOMMService) activeLocalDevice().getConnection(handle));
		long connectionHandle = s.accept();
		long remoteAddress = localDevice.getDeviceManagerService().getRemoteAddress(localDevice.getAddress(),
				connectionHandle);
		EmulatorRFCOMMClient c = localDevice.createRFCOMMClient();
		c.connect(remoteAddress, connectionHandle);
		return c.getHandle();
	}

	public void connectionRfCloseServerConnection(long handle) throws IOException {
		connectionRfCloseClientConnection(handle);
	}

	// --- Shared Client and Server RFCOMM connections

	public int connectionRfRead(long handle) throws IOException {
		return ((EmulatorRFCOMMClient) activeLocalDevice().getConnection(handle)).read();
	}

	public int connectionRfRead(long handle, byte[] b, int off, int len) throws IOException {
		return ((EmulatorRFCOMMClient) activeLocalDevice().getConnection(handle)).read(b, off, len);
	}

	public int connectionRfReadAvailable(long handle) throws IOException {
		return ((EmulatorRFCOMMClient) activeLocalDevice().getConnection(handle)).available();
	}

	public void connectionRfWrite(long handle, int b) throws IOException {
		((EmulatorRFCOMMClient) activeLocalDevice().getConnection(handle)).write(b);
	}

	public void connectionRfWrite(long handle, byte[] b, int off, int len) throws IOException {
		((EmulatorRFCOMMClient) activeLocalDevice().getConnection(handle)).write(b, off, len);
	}

	public void connectionRfFlush(long handle) throws IOException {
		((EmulatorRFCOMMClient) activeLocalDevice().getConnection(handle)).flush();
	}

	public long getConnectionRfRemoteAddress(long handle) throws IOException {
		return ((EmulatorRFCOMMClient) activeLocalDevice().getConnection(handle)).getRemoteAddress();
	}

	// --- Client and Server L2CAP connections

	private void validateMTU(int receiveMTU, int transmitMTU) {
		if (receiveMTU > localDevice.getBluetooth_l2cap_receiveMTU_max()) {
			throw new IllegalArgumentException("invalid ReceiveMTU value " + receiveMTU);
		}
	}

	public long l2OpenClientConnection(BluetoothConnectionParams params, int receiveMTU, int transmitMTU)
			throws IOException {
		validateMTU(receiveMTU, transmitMTU);
		EmulatorL2CAPClient c = activeLocalDevice().createL2CAPClient();
		boolean success = false;
		try {
			c.connect(params, receiveMTU, transmitMTU);
			success = true;
		} finally {
			if (!success) {
				localDevice.removeConnection(c);
			}
		}
		return c.getHandle();
	}

	public void l2CloseClientConnection(long handle) throws IOException {
		EmulatorL2CAPClient c = ((EmulatorL2CAPClient) localDevice.getConnection(handle));
		try {
			c.close();
		} finally {
			localDevice.removeConnection(c);
		}
	}

	public long l2ServerOpen(BluetoothConnectionNotifierParams params, int receiveMTU, int transmitMTU,
			ServiceRecordImpl serviceRecord) throws IOException {
		validateMTU(receiveMTU, transmitMTU);
		EmulatorL2CAPService s = activeLocalDevice().createL2CAPService(params.bluecove_ext_psm);
		boolean success = false;
		try {
			s.open(params, receiveMTU, transmitMTU);
			serviceRecord.setHandle(s.getHandle());
			serviceRecord.populateL2CAPAttributes((int) s.getHandle(), s.getPcm(), params.uuid, params.name);
			s.updateServiceRecord(serviceRecord);
			success = true;
		} finally {
			if (!success) {
				localDevice.removeConnection(s);
			}
		}
		return s.getHandle();
	}

	public void l2ServerUpdateServiceRecord(long handle, ServiceRecordImpl serviceRecord, boolean acceptAndOpen)
			throws ServiceRegistrationException {
		serverUpdateServiceRecord(handle, serviceRecord, acceptAndOpen);
	}

	public long l2ServerAcceptAndOpenServerConnection(long handle) throws IOException {
		EmulatorL2CAPService s = ((EmulatorL2CAPService) activeLocalDevice().getConnection(handle));
		long connectionHandle = s.accept();
		long remoteAddress = localDevice.getDeviceManagerService().getRemoteAddress(localDevice.getAddress(),
				connectionHandle);
		EmulatorL2CAPClient c = localDevice.createL2CAPClient();
		c.connect(remoteAddress, connectionHandle, s.getReceiveMTU(), s.getTransmitMTU());
		return c.getHandle();
	}

	public void l2CloseServerConnection(long handle) throws IOException {
		l2CloseClientConnection(handle);
	}

	public void l2ServerClose(long handle, ServiceRecordImpl serviceRecord) throws IOException {
		EmulatorL2CAPService s = ((EmulatorL2CAPService) localDevice.getConnection(handle));
		try {
			s.close(serviceRecord);
		} finally {
			localDevice.removeConnection(s);
		}
	}

	public int l2GetSecurityOpt(long handle, int expected) throws IOException {
		return ((EmulatorLinkedConnection) localDevice.getConnection(handle)).getSecurityOpt(expected);
	}

	public boolean l2Ready(long handle) throws IOException {
		return ((EmulatorL2CAPClient) activeLocalDevice().getConnection(handle)).ready();
	}

	public int l2Receive(long handle, byte[] inBuf) throws IOException {
		return ((EmulatorL2CAPClient) activeLocalDevice().getConnection(handle)).receive(inBuf);
	}

	public void l2Send(long handle, byte[] data) throws IOException {
		((EmulatorL2CAPClient) activeLocalDevice().getConnection(handle)).send(data);
	}

	public int l2GetReceiveMTU(long handle) throws IOException {
		return ((EmulatorL2CAPClient) activeLocalDevice().getConnection(handle)).getReceiveMTU();
	}

	public int l2GetTransmitMTU(long handle) throws IOException {
		return ((EmulatorL2CAPClient) activeLocalDevice().getConnection(handle)).getTransmitMTU();
	}

	public long l2RemoteAddress(long handle) throws IOException {
		return ((EmulatorL2CAPClient) activeLocalDevice().getConnection(handle)).getRemoteAddress();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.intel.bluetooth.BluetoothStack#l2Encrypt(long,long,boolean)
	 */
	public boolean l2Encrypt(long address, long handle, boolean on) throws IOException {
		return ((EmulatorLinkedConnection) localDevice.getConnection(handle)).encrypt(address, on);
	}
}