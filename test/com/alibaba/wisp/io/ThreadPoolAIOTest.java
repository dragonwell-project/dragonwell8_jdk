/*
 * @test
 * @summary Test thread based asynchronous IO in wisp
 * @library /lib/testlibrary
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseWisp2 -XX:+UseAsyncIO ThreadPoolAIOTest
 **/
import com.alibaba.wisp.engine.WispEngine;
import sun.misc.SharedSecrets;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.MappedByteBuffer;


import static jdk.testlibrary.Asserts.assertTrue;

public class ThreadPoolAIOTest {

    public static void testNioFileChannel(File testFile) throws Exception {
        resetTestFileContent(testFile);
        RandomAccessFile file = new RandomAccessFile(testFile, "rw");
        FileChannel ch = file.getChannel();
        ByteBuffer buffer = ByteBuffer.allocate(1);
        ch.read(buffer);
        assertTrue("0".equals(new String(buffer.array())));
        buffer.flip();
        ch.write(buffer);
        ch.close();
        String content = new String(Files.readAllBytes(testFile.toPath()));
        assertTrue("00234".equals(content));
    }

    public static void testFileStream(File testFile) throws Exception {
        //test RandomAccessFile
        resetTestFileContent(testFile);
        RandomAccessFile raf = new RandomAccessFile(testFile, "rw");
        byte[] buffer;
        buffer = "5".getBytes();
        raf.write(buffer);
        raf.seek(0);
        buffer = new byte[1];
        raf.read(buffer, 0, 1);
        assertTrue("5".equals(new String(buffer)));

        //test FileInputStream
        resetTestFileContent(testFile);
        FileInputStream fis = new FileInputStream(testFile);
        buffer = new byte[1];
        fis.read(buffer);
        assertTrue("0".equals(new String(buffer)));

        //test FileOutputStream
        resetTestFileContent(testFile);
        FileOutputStream fos = new FileOutputStream(testFile, true);
        buffer = "5".getBytes();
        fos.write(buffer);
        String content = new String(Files.readAllBytes(testFile.toPath()));
        assertTrue("012345".equals(content));

    }

    public static void testMappedByteBuffer() throws Exception {
        File newfile = new File("/tmp/ThreadPoolAioTest_test_new2.file");
        newfile.deleteOnExit();
        RandomAccessFile raf = new RandomAccessFile (newfile, "rw");
        FileChannel fc = raf.getChannel();
        MappedByteBuffer map = fc.map(FileChannel.MapMode.READ_WRITE, 0, 2048);
        fc.close();
        double current = map.getDouble (50);
        map.putDouble (50, current+0.1d);
        map.force();
    }

    public static Thread workerThread = null;

    public static void resetTestFileContent(File testFile) throws IOException {
        FileWriter writer = new FileWriter(testFile);
        for (int i = 0; i < 5; i++) {
            writer.write(String.valueOf(i));
        }
        writer.close();
    }

    public static void main(String[] args) throws Exception {

        // submit by another thread
        Thread t = new Thread(() -> {
            try {
                File f = new File("/tmp/ThreadPoolAioTest_test.file");
                f.deleteOnExit();
                // test java nio
                testNioFileChannel(f);
                // test java io
                testFileStream(f);
                // test rename
                File newfile = new File("/tmp/ThreadPoolAioTest_test_new.file");
                newfile.deleteOnExit();
                f.renameTo(newfile);
                // test MappedByteBuffer force
                testMappedByteBuffer();
                resetTestFileContent(f);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        t.start();
        t.join();
        System.out.println("Success!");
    }
}
