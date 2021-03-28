# AsmGradlePlugins

AsmGradlePlugins 是一个快速、增量、并发的 Android 字节码处理框架。它基于 [ASM](https://asm.ow2.io/) 和 [Gradle Transform API](http://tools.android.com/tech-docs/new-build-system/transform-api) 实现的，此框架对 Gradle Transform 、ASM 基础逻辑进行了封装，让开发者不在关心底层实现而更加专心于字节码处理逻辑。基于此框架您看实现注入pp性能监控（UI，网络等等），加强或修改第三方库以满足你的需求，甚至可以加强、修改Android framework的接口等功能。AsmGradlePlugins 本身支持增量、并发编译，所以不用担心使用这此框架会增加编译时间。



## License

    Copyright 2018 Quinn Chen
    
    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
    
       http://www.apache.org/licenses/LICENSE-2.0
    
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
