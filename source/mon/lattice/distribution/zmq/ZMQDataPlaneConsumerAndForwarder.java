package mon.lattice.distribution.zmq;

import mon.lattice.distribution.MeasurementDecoderWithNames;
import mon.lattice.distribution.ConsumerMeasurementWithMetaData;
import mon.lattice.distribution.MessageMetaData;
import mon.lattice.distribution.MetaData;
import mon.lattice.distribution.Receiving;
import mon.lattice.xdr.XDRDataInputStream;
import mon.lattice.distribution.MeasurementDecoder;
import mon.lattice.core.plane.MessageType;
import mon.lattice.core.plane.DataPlane;
import mon.lattice.core.Measurement;
import mon.lattice.core.MeasurementReporting;
import mon.lattice.core.ID;
import mon.lattice.core.TypeException;
import java.io.DataInput;
import java.io.ByteArrayInputStream;
import java.io.IOException;

public class ZMQDataPlaneConsumerAndForwarder extends AbstractZMQDataPlaneConsumer implements DataPlane, MeasurementReporting, Receiving {
    /**
     * Construct a UDPDataPlaneConsumerNoNames.
     */
    
    ZMQDataForwarder forwarder;
    
    public ZMQDataPlaneConsumerAndForwarder(int port) {
        super(port);
    }

    
    @Override
        public boolean connect() {
	try {
	    // only connect if we're not already connected
            if (forwarder == null) {
                    forwarder = new ZMQDataForwarder(port);
                    forwarder.startProxy();
                }
            
	    if (subscriber == null) {
                // connecting to the internal inproc
                subscriber = new ZMQDataSubscriber(this, forwarder.getInternalURI(), forwarder.getContext());
                subscriber.connect();
                subscriber.listen();
		return true;
	    } else {
		return true;
	    }

	} catch (Exception ioe) {
	    // Current implementation will be to do a stack trace
	    //ioe.printStackTrace();

	    return false;
	}

    }

    /**
     * Dicconnect from a delivery mechansim.
     */
    @Override
    public boolean disconnect() {
	try {
            forwarder.stopProxy();
	    subscriber.end();
	    subscriber = null;
	    return true;
	} catch (Exception ieo) {
	    subscriber = null;
	    return false;
	}
    }
    

    /**
     * This method is called just after a packet
     * has been received from some underlying transport
     * at a particular address.
     * The expected message is XDR encoded and it's structure is:
     * +---------------------------------------------------------------------+
     * | data source id (2 X long) | msg type (int) | seq no (int) | payload |
     * +---------------------------------------------------------------------+
     */
    public void received(ByteArrayInputStream bis, MetaData metaData) throws  IOException, TypeException {

	//System.out.println("DC: Received " + metaData);

	try {
	    DataInput dataIn = new XDRDataInputStream(bis);

	    //System.err.println("DC: datainputstream available = " + dataIn.available());

	    // get the DataSource id
            long dataSourceIDMSB = dataIn.readLong();
            long dataSourceIDLSB = dataIn.readLong();
	    ID dataSourceID = new ID(dataSourceIDMSB, dataSourceIDLSB);

	    // check message type
	    int type = dataIn.readInt();

	    MessageType mType = MessageType.lookup(type);

	    // delegate read to right object
	    if (mType == null) {
		//System.err.println("type = " + type);
		return;
	    }

	    // get seq no
	    int seq = dataIn.readInt();

	    /*
	     * Check the DataSource seq no.
	     */
	    if (seqNoMap.containsKey(dataSourceID)) {
		// we've seen this DataSource before
		int prevSeqNo = seqNoMap.get(dataSourceID);

		if (seq == prevSeqNo + 1) {
		    // we got the correct message from that DataSource
		    // save this seqNo
		    seqNoMap.put(dataSourceID, seq);
		} else {
		    // a DataSource message is missing
		    // TODO: decide what to do
		    // currently: save this seqNo
		    seqNoMap.put(dataSourceID, seq);
		}
	    } else {
		// this is a new DataSource
		seqNoMap.put(dataSourceID, seq);
	    }

	    //System.err.println("Received " + type + ". mType " + mType + ". seq " + seq);

	    // Message meta data
	    MessageMetaData msgMetaData = new MessageMetaData(dataSourceID, seq, mType);

	    // read object and check it's type
	    switch (mType) {

	    case ANNOUNCE:
		System.err.println("ANNOUNCE not implemented yet!");
		break;

	    case MEASUREMENT:
		// decode the bytes into a measurement object
		MeasurementDecoder decoder = new MeasurementDecoderWithNames();
		Measurement measurement = decoder.decode(dataIn);

		if (measurement instanceof ConsumerMeasurementWithMetaData) {
		    // add the meta data into the Measurement
		    ((ConsumerMeasurementWithMetaData)measurement).setMessageMetaData(msgMetaData);
		    ((ConsumerMeasurementWithMetaData)measurement).setTransmissionMetaData(metaData);
		}

		
		//System.err.println("DC: datainputstream left = " + dataIn.available());
		// report the measurement
		report(measurement);
		//System.err.println("DC: m = " + measurement);
		break;
	    }


	} catch (IOException ioe) {
	    System.err.println("DataConsumer: failed to process measurement input. The Measurement data is likely to be bad.");
	    throw ioe;
	} catch (Exception e) {
	    System.err.println("DataConsumer: failed to process measurement input. The Measurement data is likely to be bad.");
            throw new TypeException(e.getMessage());
	}
    }

}
