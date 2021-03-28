本目录存放的是代码静态扫描工具的配置文件。
使用说明：

1. [关于 Kotlin 静态代码检测工具 detekt 的说明](https://github.com/daishengda2018/AndroidCopywritingConverter/blob/master/%E5%85%B3%E4%BA%8E%20Kotlin%20%E9%9D%99%E6%80%81%E4%BB%A3%E7%A0%81%E6%A3%80%E6%B5%8B%E5%B7%A5%E5%85%B7%20detekt%20%E7%9A%84%E8%AF%B4%E6%98%8E.md)

2. 在 project 级 build.gradle 中添加

 ```groovy
// 1. 引入 detekt plugin
dependencies {
   classpath "io.gitlab.arturbosch.detekt:detekt-gradle-plugin:1.14.2"
}

// 2 在 allprojects 里面添加
allprojects {
    apply plugin: 'checkstyle'
    apply plugin: 'io.gitlab.arturbosch.detekt'
    checkstyle {
        toolVersion = '7.4'
    }
    task checkstyle(type: Checkstyle) {
        configFile new File(rootDir, "config/checkstyle/checkstyle.xml")
        source 'src'
        include '**/*.java'
        classpath = files()
    }
}

// 3 在文件末尾添加 
apply from: "config/detekt/detekt.gradle"
```
最后然后查看 pre-commit 文件中的说明