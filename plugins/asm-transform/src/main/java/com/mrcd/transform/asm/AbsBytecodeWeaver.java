package com.mrcd.transform.asm;

import com.android.annotations.concurrency.WorkerThread;

import org.apache.commons.io.FileUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.attribute.FileTime;
import java.util.Enumeration;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * 字节码编织器
 * 需要自己实现 {@link #getClassVisitor(ExtendClassWriter)} 按需处理字节码
 * Created by dsd on 22/3/26 18:00
 */
public abstract class AbsBytecodeWeaver {

    protected static final FileTime ZERO_TIME = FileTime.fromMillis(0);
    protected ClassLoader mClassLoader;

    @WorkerThread
    public void weaveJar(File input, File output) throws IOException {
        // 本质上 jar 包就是 zip 包，只是额外附加了一些固定的描述文件
        // https://www.liaoxuefeng.com/wiki/1252599548343744/1298366336073762
        ZipFile inputZip = new ZipFile(input);
        BufferedOutputStream outputStream = new BufferedOutputStream(Files.newOutputStream(output.toPath()));
        // 创建新的 zip 输出流
        ZipOutputStream outputZip = new ZipOutputStream(outputStream);
        Enumeration<? extends ZipEntry> inEntries = inputZip.entries();
        // 遍历元素
        while (inEntries.hasMoreElements()) {
            ZipEntry entry = inEntries.nextElement();
            // 获取输入流
            InputStream originalFile = new BufferedInputStream(inputZip.getInputStream(entry));
            // 创建新的 entry
            ZipEntry outEntry = new ZipEntry(entry.getName());
            byte[] newEntryContent;
            //  entry 的分隔符永远是 '/' 在 Windows 上也是
            String className = outEntry.getName().replace("/", ".");
            if (!isWeaveAble(className)) {
                // 不能处理直接转换待写入 output
                newEntryContent = org.apache.commons.io.IOUtils.toByteArray(originalFile);
            } else {
                newEntryContent = weaveClass(originalFile);
            }
            // 提取指纹
            CRC32 crc32 = new CRC32();
            crc32.update(newEntryContent);
            outEntry.setCrc(crc32.getValue());
            // 创建不压缩的 ZIP 存档
            outEntry.setMethod(ZipEntry.STORED);
            outEntry.setSize(newEntryContent.length);
            outEntry.setCompressedSize(newEntryContent.length);
            outEntry.setLastAccessTime(ZERO_TIME);
            outEntry.setLastModifiedTime(ZERO_TIME);
            outEntry.setCreationTime(ZERO_TIME);
            outputZip.putNextEntry(outEntry);
            outputZip.write(newEntryContent);
            outputZip.closeEntry();
        }
        outputZip.flush();
        outputZip.close();
    }

    public void setClassLoader(ClassLoader classLoader) {
        this.mClassLoader = classLoader;
    }

    /**
     * 处理 class bytecode 并写入 output
     *
     * @param input        输入文件
     * @param output       输出的文件
     * @param inputBaseDir 输入的跟地址
     */
    @WorkerThread
    public void weaveClass(File input, File output, String inputBaseDir) throws IOException {
        if (!inputBaseDir.endsWith(File.separator)) {
            inputBaseDir = inputBaseDir + File.separator;
        }
        String path = input.getAbsolutePath().replace(inputBaseDir, "")
                .replace(File.separator, ".");
        if (isWeaveAble(path)) {
            FileUtils.touch(output);
            InputStream inputStream = new FileInputStream(input);
            byte[] bytes = weaveClass(inputStream);
            FileOutputStream fos = new FileOutputStream(output);
            fos.write(bytes);
            fos.close();
            inputStream.close();
        } else if (input.isFile()) {
            FileUtils.touch(output);
            FileUtils.copyFile(input, output);
        }
    }

    /**
     * 处理 class  bytecode
     *
     * @param inputStream 文件输入流
     * @return ASM 处理后的 class byteArray
     */
    @WorkerThread
    public byte[] weaveClass(InputStream inputStream) throws IOException {
        ClassReader classReader = new ClassReader(inputStream);
        ExtendClassWriter classWriter = new ExtendClassWriter(mClassLoader, ClassWriter.COMPUTE_MAXS);
        ClassVisitor classWriterWrapper = getClassVisitor(classWriter);
        classReader.accept(classWriterWrapper, ClassReader.EXPAND_FRAMES);
        return classWriter.toByteArray();
    }

    /**
     * 获取字节码转换 ClassVisitor，用户需在此实现字节码处理逻辑
     *
     * @param classWriter 原始 ClassVisitor
     * @return 注入过 bytecode 处理逻辑的 ClassVisitor
     */
    protected abstract ClassVisitor getClassVisitor(ExtendClassWriter classWriter);


    /**
     * 是否可以进行字节码处理
     *
     * @param fullQualifiedClassName class 带有包名的全称
     */
    public boolean isWeaveAble(String fullQualifiedClassName) {
        return fullQualifiedClassName.endsWith(".class")
                && !fullQualifiedClassName.contains("R$")
                && !fullQualifiedClassName.contains("R.class")
                && !fullQualifiedClassName.contains("BuildConfig.class");
    }
}
