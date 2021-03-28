package com.mrcd.transform.asm;

import com.android.build.api.transform.DirectoryInput;
import com.android.build.api.transform.JarInput;
import com.android.build.api.transform.TransformInput;
import com.android.build.gradle.AppExtension;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

import org.gradle.api.Project;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;

/**
 * 一开始的时候我发现一个奇怪的问题：无法 hook android.jar 中的类，例如 hook 位于 android.jar 中的 Activity 不行，
 * 但 hook 位于 support 包下的 FragmentActivity 就可以。查阅资料发现android.jar 其实有两个：
 * <p>
 * 1. 第一个位于本地计算机的 android.jar（也就是位于 sdk 路径下的）这个 jar 是 Android Fragment work 的空实现，
 * 仅包含类的签名和方法，并没有任何实现代码，所有的实现都是 throw new RuntimeException("Stub!")，此 android.jar 并不参与编译
 * 也不会被打包进 apk 。所以反编译 apk 我们也找不到任何关于 android.jar 的身影。
 * <p>
 * PS: 既然都是空实现为什么通过 Android Studio 还可以看到 android.jar 的源码呢(例如 Activity 类)？
 * 那是因为你下载了 sdk 的源代码 (下载 sdk 的时候勾选了 Sources for Android XX) 只不过 AS 帮我们做了映射而已。
 * <p>
 * 2. 第二个 android.jar 位于 Android 手机上，当 apk 运行的时候依赖的就是它，他才是真正的 Android Fragment work。
 * <p>
 * 所以本地的 android.jar 在编译期是不会进行类加载的，所以我们无法 hook 里面的类。
 * 怎么解决呢？我们可以自定义个 ClassLoader 将本地 android.jar 加载进去。
 * <p>
 *
 * @see <a href="https://stackoverflow.com/questions/50283073/what-is-android-jar-what-does-it-include">What is android.jar ? What does it include?</a>
 * <p>
 * Created by im_dsd on 2021/03/28 12：04
 */
public class ClassLoaderFactory {

    public static URLClassLoader newClassLoader(
            Collection<TransformInput> inputs,
            Collection<TransformInput> referencedInputs,
            Project project) throws MalformedURLException {

        final ImmutableList.Builder<URL> urls = new ImmutableList.Builder<>();
        final File file = new File(getAndroidJarPath(project));
        final URL androidJarUrl = file.toURI().toURL();
        urls.add(androidJarUrl);
        for (TransformInput totalInputs : Iterables.concat(inputs, referencedInputs)) {
            for (DirectoryInput directoryInput : totalInputs.getDirectoryInputs()) {
                if (directoryInput.getFile().isDirectory()) {
                    urls.add(directoryInput.getFile().toURI().toURL());
                }
            }
            for (JarInput jarInput : totalInputs.getJarInputs()) {
                if (jarInput.getFile().isFile()) {
                    urls.add(jarInput.getFile().toURI().toURL());
                }
            }
        }
        ImmutableList<URL> allUrls = urls.build();
        URL[] classLoaderUrls = allUrls.toArray(new URL[0]);
        return new URLClassLoader(classLoaderUrls);
    }

    /**
     * 根据 gradle.properties 的配置获取 android.jar 路径
     * <p>
     * 例如：/Users/im_dsd/Library/Android/sdk/platforms/android-28/android.jar
     */
    protected static String getAndroidJarPath(Project project) {
        final AppExtension appExtension = (AppExtension) project.getProperties().get("android");
        final String sdkDirectory = appExtension.getSdkDirectory().getAbsolutePath();
        final String compileSdkVersion = appExtension.getCompileSdkVersion();
        final String platformDir = sdkDirectory + File.separator + "platforms" + File.separator;
        return platformDir + compileSdkVersion + File.separator + "android.jar";
    }
}
