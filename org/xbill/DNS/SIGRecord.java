// Copyright (c) 1999 Brian Wellington (bwelling@xbill.org)
// Portions Copyright (c) 1999 Network Associates, Inc.

package org.xbill.DNS;

import java.io.*;
import java.text.*;
import java.util.*;
import org.xbill.DNS.utils.*;

/**
 * Signature - A SIG provides the digital signature of an RRset, so that
 * the data can be authenticated by a DNSSEC-capable resolver.  The
 * signature is usually generated by a key contained in a KEYRecord
 * @see RRset
 * @see DNSSEC
 * @see KEYRecord
 *
 * @author Brian Wellington
 */

public class SIGRecord extends Record {

private short covered;
private byte alg, labels;
private int origttl;
private Date expire, timeSigned;
private short footprint;
private Name signer;
private byte [] signature;

private
SIGRecord() {}

/**
 * Creates an SIG Record from the given data
 * @param covered The RRset type covered by this signature
 * @param alg The cryptographic algorithm of the key that generated the
 * signature
 * @param origttl The original TTL of the RRset
 * @param expire The time at which the signature expires
 * @param timeSigned The time at which this signature was generated
 * @param footprint The footprint/key id of the signing key.
 * @param signer The owner of the signing key
 * @param signature Binary data representing the signature
 */
public
SIGRecord(Name _name, short _dclass, int _ttl, int _covered, int _alg,
	  int _origttl, Date _expire, Date _timeSigned,
	  int _footprint, Name _signer, byte [] _signature)
{
	super(_name, Type.SIG, _dclass, _ttl);
	covered = (short) _covered;
	alg = (byte) _alg;
	labels = name.labels();
	origttl = _origttl;
	expire = _expire;
	timeSigned = _timeSigned;
	footprint = (short) _footprint;
	signer = _signer;
	signature = _signature;
}

SIGRecord(Name _name, short _dclass, int _ttl, int length,
	  DataByteInputStream in)
throws IOException
{
	super(_name, Type.SIG, _dclass, _ttl);
	if (in == null)
		return;
	int start = in.getPos();
	covered = in.readShort();
	alg = in.readByte();
	labels = in.readByte();
	origttl = in.readInt();
	expire = new Date(1000 * (long)in.readInt());
	timeSigned = new Date(1000 * (long)in.readInt());
	footprint = in.readShort();
	signer = new Name(in);
	signature = new byte[length - (in.getPos() - start)];
	in.read(signature);
}

SIGRecord(Name _name, short _dclass, int _ttl, MyStringTokenizer st,
	     Name origin)
throws IOException
{
	super(_name, Type.SIG, _dclass, _ttl);
	covered = Type.value(st.nextToken());
	alg = Byte.parseByte(st.nextToken());
	if (Options.check("2065sig"))
		labels = name.labels();
	else
		labels = Byte.parseByte(st.nextToken());
	origttl = TTL.parseTTL(st.nextToken());
	expire = parseDate(st.nextToken());
	timeSigned = parseDate(st.nextToken());
	footprint = (short) Integer.parseInt(st.nextToken());
	signer = Name.fromString(st.nextToken(), origin);
	if (st.hasMoreTokens())
		signature = base64.fromString(st.remainingTokens());
}

/** Converts rdata to a String */
public String
rdataToString() {
	StringBuffer sb = new StringBuffer();
	if (signature != null) {
		sb.append (Type.string(covered));
		sb.append (" ");
		sb.append (alg);
		sb.append (" ");
		if (!Options.check("2065sig")) {
			sb.append (labels);
			sb.append (" ");
		}
		sb.append (origttl);
		sb.append (" (\n\t");
		sb.append (formatDate(expire));
		sb.append (" ");
		sb.append (formatDate(timeSigned));
		sb.append (" ");
		sb.append ((int)footprint & 0xFFFF);
		sb.append (" ");
		sb.append (signer);
		sb.append ("\n");
		sb.append (base64.formatString(signature, 64, "\t", true));
        }
	return sb.toString();
}

/** Returns the RRset type covered by this signature */
public short
getTypeCovered() {
	return covered;
}

/**
 * Returns the cryptographic algorithm of the key that generated the signature
 */
public byte
getAlgorithm() {
	return alg;
}

/**
 * Returns the number of labels in the signed domain name.  This may be
 * different than the record's domain name if the record is a wildcard
 * record.
 */
public byte
getLabels() {
	return labels;
}

/** Returns the original TTL of the RRset */
public int
getOrigTTL() {
	return origttl;
}

/** Returns the time at which the signature expires */
public Date
getExpire() {
	return expire;
}

/** Returns the time at which this signature was generated */
public Date
getTimeSigned() {
	return timeSigned;
}

/** Returns The footprint/key id of the signing key.  */
public short
getFootprint() {
	return footprint;
}

/** Returns the owner of the signing key */
public Name
getSigner() {
	return signer;
}

/** Returns the binary data representing the signature */
public byte []
getSignature() {
	return signature;
}

void
rrToWire(DataByteOutputStream out, Compression c) throws IOException {
	if (signature == null)
		return;

	out.writeShort(covered);
	out.writeByte(alg);
	out.writeByte(labels);
	out.writeInt(origttl);
	out.writeInt((int)(expire.getTime() / 1000));
	out.writeInt((int)(timeSigned.getTime() / 1000));
	out.writeShort(footprint);
	signer.toWire(out, null);
	out.write(signature);
}

void
rrToWireCanonical(DataByteOutputStream out) throws IOException {
	if (signature == null)
		return;

	out.writeShort(covered);
	out.writeByte(alg);
	out.writeByte(labels);
	out.writeInt(origttl);
	out.writeInt((int)(expire.getTime() / 1000));
	out.writeInt((int)(timeSigned.getTime() / 1000));
	out.writeShort(footprint);
	signer.toWireCanonical(out);
	out.write(signature);
}

static String
formatDate(Date d) {
	Calendar c = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
	StringBuffer sb = new StringBuffer();
	NumberFormat w4 = new DecimalFormat();
	w4.setMinimumIntegerDigits(4);
	w4.setGroupingUsed(false);
	NumberFormat w2 = new DecimalFormat();
	w2.setMinimumIntegerDigits(2);

	c.setTime(d);
	sb.append(w4.format(c.get(c.YEAR)));
	sb.append(w2.format(c.get(c.MONTH)+1));
	sb.append(w2.format(c.get(c.DAY_OF_MONTH)));
	sb.append(w2.format(c.get(c.HOUR_OF_DAY)));
	sb.append(w2.format(c.get(c.MINUTE)));
	sb.append(w2.format(c.get(c.SECOND)));
	return sb.toString();
}

static Date
parseDate(String s) {
	Calendar c = new GregorianCalendar(TimeZone.getTimeZone("UTC"));

	int year = Integer.parseInt(s.substring(0, 4));
	int month = Integer.parseInt(s.substring(4, 6)) - 1;
	int date = Integer.parseInt(s.substring(6, 8));
	int hour = Integer.parseInt(s.substring(8, 10));
	int minute = Integer.parseInt(s.substring(10, 12));
	int second = Integer.parseInt(s.substring(12, 14));
	c.set(year, month, date, hour, minute, second);

	return c.getTime();
}

}
