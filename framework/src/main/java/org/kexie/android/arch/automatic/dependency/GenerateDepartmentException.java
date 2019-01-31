package org.kexie.android.arch.automatic.dependency;

import org.dom4j.Element;

/**
 * Created by Mr.小世界 on 2018/11/30.
 */

public class GenerateDepartmentException
        extends RuntimeException
{
    public GenerateDepartmentException(Element element, Throwable e)
    {
        super("in [" + element.toString() + "] : ",e);
    }

    public GenerateDepartmentException(Element element, String message)
    {
        super("in [" + element.toString() + "] : " + message);
    }

    public GenerateDepartmentException(Throwable e)
    {
        super(e);
    }

    public GenerateDepartmentException(String message)
    {
        super(message);
    }
}
