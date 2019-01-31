package org.kexie.android.arch.automatic.dependency;

import org.dom4j.Element;

/**
 * Created by Mr.小世界 on 2018/11/30.
 */

final class GenerateDepartmentException
        extends RuntimeException
{
    GenerateDepartmentException(Element element, Throwable e)
    {
        super("in [" + element.toString() + "] : ",e);
    }

    GenerateDepartmentException(Element element, String message)
    {
        super("in [" + element.toString() + "] : " + message);
    }
}
