package com.mrcd.transform.asm;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

/**
 * ClassWriter其中的一个逻辑，寻找两个类的共同父类。可以看看 {@link ClassWriter#getCommonSuperClass(String, String)} 方法
 * 而编译期间使用的 classloader 并没有加载 android.jar 中的代码，所以我们需要一个自定义的 ClassLoader 加载它
 * <p>
 * 详情见 {@link ClassLoaderFactory#getAndroidJarPath(org.gradle.api.Project)}
 * <p>
 * Create by im_dsd 2021/3/28 15：07
 */
public class ExtendClassWriter extends ClassWriter {
    public static final String TAG = "ExtendClassWriter";
    protected static final String OBJECT = "java/lang/Object";
    protected final ClassLoader mUrlClassLoader;

    public ExtendClassWriter(ClassLoader urlClassLoader, int flags) {
        super(flags);
        this.mUrlClassLoader = urlClassLoader;
    }

    /**
     * {@link ClassWriter#getCommonSuperClass(String, String)} 是如何寻找父类的呢？它是通过 Class.forName 加载某个类，然后再去寻找父类。
     * Class.froName 加载类有个特点：它会触发类的静态域初始化，但 android.jar 可不是说加载就能加载的，因为其内部的空实现都抛出了
     * RuntimeException("Stub!") 异常。如果加载的类里面有的静态域(static)且指向的是空实现，那么一旦初始化就会抛出 RuntimeException。
     * <p>
     * 那怎么寻找当前类的父类呢？答案就在当前类的字节码里面，父类信息也存在当前字节码文件中，我们只要读出来即可。
     * https://github.com/Moniter123/pinpoint/blob/40106ffe6cc4d6aea9d59b4fb7324bcc009483ee/profiler/src/main/java/com/navercorp/pinpoint/profiler/instrument/ASMClassWriter.java
     */
    @Override
    protected String getCommonSuperClass(final String type1, final String type2) {
        if (type1 == null || type1.equals(OBJECT) || type2 == null || type2.equals(OBJECT)) {
            return OBJECT;
        }
        if (type1.equals(type2)) {
            return type1;
        }

        ClassReader type1ClassReader = getClassReader(type1);
        ClassReader type2ClassReader = getClassReader(type2);
        if (type1ClassReader == null || type2ClassReader == null) {
            return OBJECT;
        }

        if (isInterface(type1ClassReader)) {
            if (isImplements(type1, type2ClassReader)) {
                return type1;
            }
            if (isInterface(type2ClassReader) && isImplements(type2, type1ClassReader)) {
                return type2;
            }
            return OBJECT;
        }

        if (isInterface(type2ClassReader)) {
            if (isImplements(type2, type1ClassReader)) {
                return type2;
            }
            return OBJECT;
        }

        final Set<String> superClassNames = new HashSet<>();
        superClassNames.add(type1);
        superClassNames.add(type2);

        String type1SuperClassName = type1ClassReader.getSuperName();
        if (!superClassNames.add(type1SuperClassName)) {
            return type1SuperClassName;
        }
        String type2SuperClassName = type2ClassReader.getSuperName();
        if (!superClassNames.add(type2SuperClassName)) {
            return type2SuperClassName;
        }

        while (type1SuperClassName != null || type2SuperClassName != null) {
            if (type1SuperClassName != null) {
                type1SuperClassName = getSuperClassName(type1SuperClassName);
                if (type1SuperClassName != null && !superClassNames.add(type1SuperClassName)) {
                    return type1SuperClassName;
                }
            }

            if (type2SuperClassName != null) {
                type2SuperClassName = getSuperClassName(type2SuperClassName);
                if (type2SuperClassName != null && !superClassNames.add(type2SuperClassName)) {
                    return type2SuperClassName;
                }
            }
        }

        return OBJECT;
    }

    protected boolean isImplements(final String interfaceName, final ClassReader classReader) {
        ClassReader classInfo = classReader;

        while (classInfo != null) {
            final String[] interfaceNames = classInfo.getInterfaces();
            for (String name : interfaceNames) {
                if (name != null && name.equals(interfaceName)) {
                    return true;
                }
            }

            for (String name : interfaceNames) {
                if (name != null) {
                    final ClassReader interfaceInfo = getClassReader(name);
                    if (interfaceInfo != null) {
                        if (isImplements(interfaceName, interfaceInfo)) {
                            return true;
                        }
                    }
                }
            }

            final String superClassName = classInfo.getSuperName();
            if (superClassName == null || superClassName.equals(OBJECT)) {
                break;
            }
            classInfo = getClassReader(superClassName);
        }

        return false;
    }

    protected boolean isInterface(final ClassReader classReader) {
        return (classReader.getAccess() & Opcodes.ACC_INTERFACE) != 0;
    }


    protected String getSuperClassName(final String className) {
        final ClassReader classReader = getClassReader(className);
        if (classReader == null) {
            return null;
        }
        return classReader.getSuperName();
    }

    protected ClassReader getClassReader(final String className) {
        try (InputStream inputStream = mUrlClassLoader.getResourceAsStream(className + ".class")) {
            if (inputStream != null) {
                return new ClassReader(inputStream);
            }
        } catch (IOException ignored) {
        }
        return null;
    }
}

