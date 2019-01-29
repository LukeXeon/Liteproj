package org.kexie.android.arch.automatic.databinding;

import android.databinding.ViewDataBinding;
import android.text.TextUtils;
import android.util.ArrayMap;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

public final class DataBindingTool
{
    private static final Map<Class<? extends ViewDataBinding>,
            Map<String, Method>> TABLE = new ArrayMap<>();

    private static String toFirstUpCaseName(String variableName)
    {
        return String.valueOf(
                Character.toUpperCase(variableName.charAt(0))
                        + variableName.substring(1));
    }

    @SuppressWarnings("WeakerAccess")
    public static void setVariable(ViewDataBinding binding,
                                   String variableName,
                                   Object data)
    {
        if (TextUtils.isEmpty(variableName))
        {
            throw new IllegalArgumentException();
        }
        try
        {
            Class<? extends ViewDataBinding> bindingClass = binding.getClass();
            Map<String, Method> table = TABLE.get(binding.getClass());
            if (table == null)
            {
                table = new ArrayMap<>();
                TABLE.put(bindingClass, table);
            }
            variableName = "set" + toFirstUpCaseName(variableName);
            Method setter = table.get(variableName);
            if (setter == null)
            {
                for (Method method : binding.getClass().getMethods())
                {
                    Class<?>[] parameters = method.getParameterTypes();
                    if (method.getName().equals(variableName)
                            && parameters.length == 1
                            && parameters[0].isInstance(data)
                            && void.class.equals(method.getReturnType()))
                    {
                        setter = method;
                        table.put(variableName, setter);
                    }
                }
                if (setter == null)
                {
                    throw new RuntimeException(variableName + " Setter not found");
                }
            }
            setter.invoke(binding, data);
        } catch (IllegalAccessException | InvocationTargetException e)
        {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T getVariable(ViewDataBinding binding,
                                    String variableName)
    {
        variableName = "get" + toFirstUpCaseName(variableName);
        if (TextUtils.isEmpty(variableName))
        {
            throw new IllegalArgumentException();
        }
        try
        {
            Class<? extends ViewDataBinding> bindingClass = binding.getClass();
            Map<String, Method> table = TABLE.get(bindingClass);
            if (table == null)
            {
                table = new ArrayMap<>();
                TABLE.put(bindingClass, table);
            }

            Method getter = table.get(variableName);
            if (getter == null)
            {
                for (Method method : binding.getClass().getMethods())
                {
                    if (method.getName().equals(variableName)
                            && method.getParameterTypes().length == 0
                            && !void.class.equals(method.getReturnType()))
                    {
                        getter = method;
                        table.put(variableName, getter);
                    }
                }
                if (getter == null)
                {
                    throw new RuntimeException(variableName + " Getter not found");
                }
            }
            return (T) getter.invoke(binding);
        } catch (IllegalAccessException | InvocationTargetException e)
        {
            throw new RuntimeException(e);
        }
    }
}
