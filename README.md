# Liteproj

![](https://jitpack.io/v/LukeXeon/Liteproj.svg)
[![](https://img.shields.io/badge/license-MIT-FF0080.svg)](https://github.com/LukeXeon/Liteproj/blob/master/LICENSE)

Liteproj是一个运行在Android上轻量级的依赖注入框架，它使用xml进行依赖配置，并允许对Android中的各种组件进行依赖注入。

## 如何使用？

与绝大多数Android插件相同，它使用gradle进行构建，若想使用它你的项目必须依赖appcompat组件包，并且需要在build.gradle文件中添加如下代码。

```
	allprojects {
		repositories {
			...
			maven { url 'https://www.jitpack.io' }
		}
	}

	dependencies {
	        implementation 'com.github.LukeXeon:Liteproj:+'
	}
```

但它又与绝大多数Android插件不同，它不需要你在Activity或者Application中进行初始化（对的，不需要自定义的Application类，也不需要你去调用奇怪的init方法再传入一个Context实例），框架内部已经使用ContentProvider自动完成了初始化，并能够在多进程的情况下正常工作，你完全不必关注初始化问题。

接下来，你需要像这样编写依赖配置的xml文件：

```xml
<?xml version="1.0" encoding="utf-8"?>
<dependency owner="android.app.Application">
    <var name="url" val="@http://www.hao123.com"/>
    <var
        name="request"
        type="okhttp3.Request"
        provider="singleton">
        <builder
            type="okhttp3.Request$Builder">
            <arg name="url" ref="url"/>
        </builder>
    </var>
    <var
        name="bean"
        type="org.kexie.android.liteproj.sample.Bean"
        provider="factory">
        <factory
            action="test"
            type="org.kexie.android.liteproj.sample.Factory">
            <arg val="@asdasdd"/>
        </factory>
        <field
            name="field"
            val="100"/>
        <field
            name="string"
            val="@adadadad"/>
        <property
            name="object"
            ref="owner"/>
    </var>
    <var
        name="holder"
        type="org.kexie.android.liteproj.sample.AppHolderTest">
        <new>
            <arg ref="owner"/>
        </new>
    </var>
</dependency>
```

并在Java中使用注解标记Activity：

```java
@Using({R.xml.all_test})
public class MainActivity extends AppCompatActivity
{
    private static final String TAG = "MainActivity";

    @Reference("request")
    Request request;

    @Reference("bean")
    Bean bean;

    @Reference("holder")
    AppHolderTest holderTest;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Logger.d(request + "\n" + bean + "\n" + holderTest.context);
    }
}
```

然后直接运行你的App，就可以看到这些对象居然都被自动设置好了，是不是很神奇？

## 使用指南

请看[博文分析](https://juejin.im/post/5c9439b55188252d856302b6)

## 为什么会有这个库？

有一句话说得好，那就是不要重复发明轮子，也许有人会问我，Android上不是已经有Google开发Dagger2作为依赖注入插件使用了吗？没错，是这样的，但这也是我开发这款插件的原因之一，如果用过Dagger2的同学应该知道，Dagger2的依赖关系是使用java硬编码来实现的，但是我相信包括我在内的很多用过Spring framework的同学一定更习惯于使用xml来配置依赖注入，所以就有了这个库的诞生。

使用xml是有优势的，xml是最常见的配置文件，它能更明确的表达依赖关系。Liteproj的目前的实现中也没有使用注解处理器而是使用了反射，因为Liteproj追求的并非是极致的性能，而是功能、轻量化和易用性，它的诞生并不是为了取代Dagger2或者其他的一些依赖注入工具，而是在它们所没有涉及的领域做一个补全。目前Liteproj还在开发中，可能会存在一些问题，我会保持高频率的更新。如果你在使用这个库时发现了某个问题，欢迎给我留言，我们可以共同解决。

## 感谢

- xgouchet/AXML https://github.com/xgouchet/AXML

- dom4j/dom4j https://github.com/dom4j/dom4j

## 使用MIT协议

```
MIT License

Copyright (c) 2019 Luke

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```
