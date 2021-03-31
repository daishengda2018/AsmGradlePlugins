package com.mrcd.transform;

import com.android.build.api.transform.Context;
import com.android.build.api.transform.DirectoryInput;
import com.android.build.api.transform.Format;
import com.android.build.api.transform.JarInput;
import com.android.build.api.transform.QualifiedContent;
import com.android.build.api.transform.Status;
import com.android.build.api.transform.Transform;
import com.android.build.api.transform.TransformException;
import com.android.build.api.transform.TransformInput;
import com.android.build.api.transform.TransformInvocation;
import com.android.build.api.transform.TransformOutputProvider;
import com.android.build.gradle.internal.pipeline.TransformManager;
import com.mrcd.transform.asm.AbsBytecodeWeaver;
import com.mrcd.transform.asm.ClassLoaderFactory;
import com.mrcd.transform.concurrent.Schedulers;
import com.mrcd.transform.concurrent.Worker;

import org.apache.commons.io.FileUtils;
import org.gradle.api.Project;
import org.gradle.api.logging.Logger;

import java.io.File;
import java.io.IOException;
import java.net.URLClassLoader;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Gradle Transform 入口，修改项目的字节码
 * Created by im_dsd on 2021/3/26
 */
public class BaseTransform extends Transform {
    public static final String DEBUG = "debug";
    public static final String RELEASE = "release";
    protected final Logger mLogger;
    protected final Project mProject;
    protected AbsBytecodeWeaver mBytecodeResolver;
    protected final Worker mWorker;
    protected boolean isEmptyRun = false;
    protected boolean shouldCleanDexBuilderFolder = false;

    public BaseTransform(Project project, AbsBytecodeWeaver handler) {
        mBytecodeResolver = handler;
        this.mProject = project;
        this.mLogger = project.getLogger();
        this.mWorker = Schedulers.newWorker();
    }

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }

    @Override
    public Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS;
    }

    @Override
    public Set<QualifiedContent.ScopeType> getScopes() {
        return TransformManager.SCOPE_FULL_PROJECT;
    }

    @Override
    public boolean isIncremental() {
        return true;
    }

    @Override
    public void transform(final TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {
        super.transform(transformInvocation);
        long startTime = System.currentTimeMillis();

        Context context = transformInvocation.getContext();
        Collection<TransformInput> inputs = transformInvocation.getInputs();
        Collection<TransformInput> referencedInputs = transformInvocation.getReferencedInputs();
        TransformOutputProvider outputProvider = transformInvocation.getOutputProvider();
        boolean isIncremental = transformInvocation.isIncremental();

        RunVariant runVariant = getRunVariant();
        setupRunVariant(context);
        mLogger.warn(getName() + " isIncremental = " + isIncremental + ", runVariant = "
                + runVariant + ", emptyRun = " + isEmptyRun + ", inDuplicatedClassSafeMode = " + inDuplicatedClassSafeMode());

        // 不是增量编译需要清除所有文件
        if (!isIncremental) {
            outputProvider.deleteAll();
        }
        URLClassLoader urlClassLoader = ClassLoaderFactory.newClassLoader(inputs, referencedInputs, mProject);
        this.mBytecodeResolver.setClassLoader(urlClassLoader);

        shouldCleanDexBuilderFolder = false;
        // 处理输入文件
        for (TransformInput input : inputs) {
            for (JarInput jarInput : input.getJarInputs()) {
                transformJarInput(transformInvocation, jarInput);
            }

            for (DirectoryInput directoryInput : input.getDirectoryInputs()) {
                transformDirectoryInput(transformInvocation, directoryInput);
            }
        }

        mWorker.await();
        long costTime = System.currentTimeMillis() - startTime;
        mLogger.warn((getName() + " costed " + costTime + "ms"));
    }

    /**
     * 处理单个的 jar input
     *
     * @throws IOException
     */
    protected void transformJarInput(final TransformInvocation transformInvocation, final JarInput jarInput) throws IOException {
        final TransformOutputProvider outputProvider = transformInvocation.getOutputProvider();
        final boolean isIncremental = transformInvocation.isIncremental();
        final Status status = jarInput.getStatus();
        final String name = jarInput.getFile().getAbsolutePath();
        final File dest = getContentLocation(outputProvider, jarInput, name, Format.JAR);
        if (isIncremental && !isEmptyRun) {
            switch (status) {
                case ADDED:
                case CHANGED:
                    transformJar(jarInput.getFile(), dest);
                    break;
                case REMOVED:
                    if (dest.exists()) {
                        FileUtils.forceDelete(dest);
                    }
                    break;
                case NOTCHANGED:
                default:
                    break;
            }
        } else {
            // Forgive me!, Some project will store 3rd-party aar for several copies in dexbuilder folder,unknown issue.
            if (inDuplicatedClassSafeMode() && !isIncremental && !shouldCleanDexBuilderFolder) {
                cleanDexBuilderFolder(dest);
                shouldCleanDexBuilderFolder = true;
            }
            transformJar(jarInput.getFile(), dest);
        }
    }

    protected void transformJar(final File input, final File output) {
        mWorker.submit(() -> {
            if (isEmptyRun) {
                FileUtils.copyFile(input, output);
                return null;
            }
            mBytecodeResolver.weaveJar(input, output);
            return null;
        });
    }

    protected void transformDirectoryInput(final TransformInvocation transformInvocation, final DirectoryInput directoryInput) throws IOException {
        final TransformOutputProvider outputProvider = transformInvocation.getOutputProvider();
        final boolean isIncremental = transformInvocation.isIncremental();
        final File dest = getContentLocation(outputProvider, directoryInput, directoryInput.getName(), Format.DIRECTORY);
        FileUtils.forceMkdir(dest);
        if (!isIncremental || isEmptyRun) {
            transformDir(directoryInput.getFile(), dest);
            return;
        }
        final String srcDirPath = directoryInput.getFile().getAbsolutePath();
        final String destDirPath = dest.getAbsolutePath();
        final Map<File, Status> fileStatusMap = directoryInput.getChangedFiles();
        for (Map.Entry<File, Status> changedFile : fileStatusMap.entrySet()) {
            transferChangedFiles(srcDirPath, destDirPath, changedFile);
        }
    }

    protected void transformDir(final File inputDir, final File outputDir) throws IOException {
        if (isEmptyRun) {
            FileUtils.copyDirectory(inputDir, outputDir);
            return;
        }
        final boolean isDire = inputDir.isDirectory();
        if (!isDire) {
            return;
        }
        final String inputDirPath = inputDir.getAbsolutePath();
        final String outputDirPath = outputDir.getAbsolutePath();
        for (final File file : com.android.utils.FileUtils.getAllFiles(inputDir)) {
            mWorker.submit(() -> {
                String filePath = file.getAbsolutePath();
                File outputFile = new File(filePath.replace(inputDirPath, outputDirPath));
                mBytecodeResolver.weaveClass(file, outputFile, inputDirPath);
                return null;
            });
        }
    }

    protected void transferChangedFiles(final String srcDirPath, final String destDirPath,
                                        final Map.Entry<File, Status> changedFile) throws IOException {
        final Status status = changedFile.getValue();
        final File inputFile = changedFile.getKey();
        final String destFilePath = inputFile.getAbsolutePath().replace(srcDirPath, destDirPath);
        final File destFile = new File(destFilePath);
        switch (status) {
            case ADDED:
            case CHANGED:
                try {
                    FileUtils.touch(destFile);
                } catch (IOException e) {
                    // maybe mkdirs fail for some strange reason, try again.
                    FileUtils.forceMkdirParent(destFile);
                }
                transformSingleFile(inputFile, destFile, srcDirPath);
                break;
            case REMOVED:
                if (destFile.exists()) {
                    destFile.delete();
                }
                break;
            case NOTCHANGED:
            default:
                break;
        }
    }

    protected File getContentLocation(TransformOutputProvider outputProvider, QualifiedContent input, String name, Format format) {
        return outputProvider.getContentLocation(
                name,
                input.getContentTypes(),
                input.getScopes(),
                format
        );
    }

    protected void setupRunVariant(final Context context) {
        final RunVariant runVariant = getRunVariant();
        if (DEBUG.equals(context.getVariantName())) {
            isEmptyRun = (runVariant == RunVariant.RELEASE || runVariant == RunVariant.NEVER);
        } else if (RELEASE.equals(context.getVariantName())) {
            isEmptyRun = (runVariant == RunVariant.DEBUG || runVariant == RunVariant.NEVER);
        }
    }

    protected void transformSingleFile(final File src, final File des, final String srcBaseDir) {
        mWorker.submit(() -> {
            mBytecodeResolver.weaveClass(src, des, srcBaseDir);
            return null;
        });
    }


    protected void cleanDexBuilderFolder(File dest) {
        mWorker.submit(() -> {
            try {
                String dexBuilderDir = replaceLastPart(dest.getAbsolutePath(), getName(), "dexBuilder");
                // intermediates/transforms/dexBuilder/debug
                File file = new File(dexBuilderDir).getParentFile();
                mProject.getLogger().warn("clean dexBuilder folder = " + file.getAbsolutePath());
                if (file.exists() && file.isDirectory()) {
                    com.android.utils.FileUtils.deleteDirectoryContents(file);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        });
    }

    protected String replaceLastPart(String originString, String replacement, String toreplace) {
        int start = originString.lastIndexOf(replacement);
        StringBuilder builder = new StringBuilder();
        builder.append(originString, 0, start);
        builder.append(toreplace);
        builder.append(originString.substring(start + replacement.length()));
        return builder.toString();
    }

    @Override
    public boolean isCacheable() {
        return true;
    }

    protected RunVariant getRunVariant() {
        return RunVariant.ALWAYS;
    }

    protected boolean inDuplicatedClassSafeMode() {
        return false;
    }
}
