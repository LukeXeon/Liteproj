# Liteproj
- 历经半个学期我的开源框架终于发布了Preview版本，这个开源框架是一个运行在Android上的控制反转（IoC）容器，能够帮助你自动的管理类的依赖并提供依赖注入（DI）。
- 与绝大多数Android插件相同，它使用Gradle进行构建，若想要使用它你需要在build.gradle文件中添加如下代码。

```
	allprojects {
		repositories {
			...
			maven { url 'https://www.jitpack.io' }
		}
	}

	dependencies {
	        implementation 'com.github.LukeXeon:kexie-android-arch:+'
	}
```
- 但它又与绝大多数Android插件不同，它不需要你在Activity或者Application中进行初始化（对的，不需要自定义的Application类，也不需要你去调用奇怪的init方法再传入一个Context实例），它也没有使用注解处理器，而是完全依赖AppCompat组件包和反射实现，并使用ContentProvider来hook程序的启动流程完成自身初始化。
- 有一句话说得好，那就是不要重复发明轮子，也许有人会问我，Android上不是已经又Google开发Dagger2作为依赖注入插件使用了吗？没错，是这样的，但这也是我开发这款插件的原因之一，如果用过Dagger2的同学应该知道，Dagger2的依赖关系是使用java硬编码来实现的，但是我相信包括我在内的很多用过Spring framework的同学一定更习惯于使用xml来配置依赖注入，所以就有了这个库的诞生。
- 注意，此库必须依赖AppCompat组件包且最好时26以上的，这意味着，你所使用的Activity必须是AppCompatActivity，你所使用的Fragment必须是android.support.v4.app.Fragment，Application则没有限制，目前此插件只支持三种类的依赖注入，分别是AppCompatActivity的子类，v4包下Fragment的子类，以及Application的子类，并且xml文件只支持res/raw文件夹下的xml文件。依赖注入会在onCreate调用时完成。

你可以像这样编写xml文件。

```xml
<?xml version="1.0" encoding="utf-8"?>
<scope owner="org.kexie.android.liteproj.sample.MainActivity">
    <var
        name="bean"
        class="org.kexie.android.liteproj.sample.Bean"
        provider="singleton"/>
    <var
        name="factory"
        class="org.kexie.android.liteproj.sample.Factory"
        provider="factory">
        <new name="test">
            <arg let="@xxxxxxxx"/>
        </new>
        <field
            name="field"
            let="123.1"/>
        <field
            name="string"
            let="@kexie niu bi"/>
        <property
            name="Object"
            ref="bean"/>
    </var>
</scope>
```

然后定义一个javabean对象

```java
public class Bean
{
    public float field;

    public String string;

    Object object;

    public void setObject(Object object)
    {
        this.object = object;
    }
}
```

并在MainActivity中使用@Using注解来引用res/raw文件夹下的xml文件，并使用  @Reference("use by name") 来引用xml中的依赖，然后直接运行你的App，就可以看到这些对象居然都被自动设置好了，是不是很神奇？

```java
@Using(R.raw.test_main)
public class MainActivity extends AppCompatActivity
{
    private static final String TAG = "MainActivity";

    @Reference("factory")
    private Bean o;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Logger.d("" + o + "  " + o.string + "  " + o.object);
    }
}
```
- 如何编写配置文件？这是很简单的，如果你有过编写Spring MVC的经验，相信你会很快上手。
- 在xml中，你可以使用var定义一个变量提供器，并用name指定他在上下文的唯一名字，用class指定它的类型，用provider指定它的提供模式有singleton（单例）和factory两种模式，singleton保证每次都返回同一对象，而factory保证每次都创建新的对象。
- 在var便签，之下还可以用new标签指定其构造函数，arg 标签可以引用xml上下文中的定义变量或常量，使用ref引用变量，使用let引用常量，当let以@开头时识别为字符串，否则当作数字处理。
- 也许你已经发现了xml scope中的owner 属性，它是必须的，owner规定了这个依赖关系只能被哪种类型所使用，使用@Using注解时必须与owner所指定的类型所匹配，但是也有例外，@Using注解可以以数组的形式添加多个xml资源，AppCompatActivity可以引用owner为Application的xml，Fragment可以引用AppCompatActivity和Application的xml，框架会自动处理他们之间的联系，你无需关心，必要时，可以在xml中使用ref=“owner”对象来获取依赖的持有者（比如当你需要一个Activity的Context时）。
- 差不多就这些了，github地址：https://github.com/LukeXeon/kexie-android-arch 接下来还会持续更新并完善文档吧，让功能尽快稳定下来，让它变成一个能用，敢用的框架。

依赖：
```
com.android.support:appcompat-v7:+
com.android.support:design:+
android.arch.lifecycle:extensions:+
com.github.CymChad:BaseRecyclerViewAdapterHelper:+
org.dom4j:dom4j:+
```

USE MIT LICENSE

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
