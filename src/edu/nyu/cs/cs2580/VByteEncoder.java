package edu.nyu.cs.cs2580;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;



/**
 * Created by sanchitmehta on 31/10/16.
 */
public class VByteEncoder {

    private VByteEncoder() {}

    public static void encode(OutputStream out, long value) throws IOException {
        if(value<0) {
            throw new IllegalArgumentException("Only can encode VByte of positive values");
        }
        while( value > 127) {
            out.write((int)(value & 127));
            value>>>=7;
        }
        out.write((int)(value|0x80));
    }

    public static long decode(InputStream in) throws IOException {
        long out = 0;
        int shift=0;
        long readbyte = in.read(); if(readbyte==-1) throw new EOFException();

        while( (readbyte & 0x80)==0) {
            if(shift>=50) { // We read more bytes than required to load the max long
                throw new IllegalArgumentException();
            }

            out |= (readbyte & 127) << shift;

            readbyte = in.read(); if(readbyte==-1) throw new EOFException();

            shift+=7;
        }
        out |= (readbyte & 127) << shift;
        return out;
    }

    public static long decode(ByteBuffer in) throws IOException {
        long out = 0;
        int shift=0;
        if(!in.hasRemaining()) throw new EOFException();
        byte readbyte = in.get();

        while( (readbyte & 0x80)==0) {
            if(shift>=50) { // We read more bytes than required to load the max long
                throw new IllegalArgumentException();
            }

            out |= (readbyte & 127) << shift;

            if(!in.hasRemaining()) throw new EOFException();
            readbyte = in.get();

            shift+=7;
        }
        out |= (readbyte & 127) << shift;
        return out;
    }

    public static int encode(byte[] data, int offset, int value) {
        if(value<0) {
            throw new IllegalArgumentException("Only can encode VByte of positive values");
        }
        int i=0;
        while( value > 127) {
            data[offset+i] = (byte)(value & 127);
            i++;
            value>>>=7;
        }
        data[offset+i] = (byte)(value|0x80);
        i++;

        return i;
    }

    public static int decode(byte[] data, int offset, Mutable<Long> value) {
        long out = 0;
        int i=0;
        int shift=0;
        while( (0x80 & data[offset+i])==0) {
            out |= (data[offset+i] & 127) << shift;
            i++;
            shift+=7;
        }
        out |= (data[offset+i] & 127) << shift;
        i++;
        value.setValue(out);
        return i;
    }

    public static void show(byte[] data, int len) {
        for(int i=0;i<len;i++) {
            System.out.print(Long.toHexString(data[i]&0xFF)+" ");
        }
    }

}