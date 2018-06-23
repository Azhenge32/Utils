package com.azhen;

import com.esotericsoftware.reflectasm.MethodAccess;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class BeanUtils {

    public static void copyProperties(Object src, Object dest, String... properties) {
        Class srcClass = src.getClass();
        Class destClass = dest.getClass();
        MethodAccess srcMethodAccess = MethodAccess.get(srcClass);
        MethodAccess destMethodAccess = MethodAccess.get(destClass);
        try {
            for (String property : properties) {
                    /*Method srcMethod = srcClass.getMethod("get" + upperFirstLetter(property));
                    Class retType = srcMethod.getReturnType();
                    Method destMethod = destClass.getMethod("set" + upperFirstLetter(property), retType);
                    destMethod.invoke(dest, srcMethod.invoke(src, null));*/
                int srcIdx = srcMethodAccess.getIndex("get" + upperFirstLetter(property));
                int destIdx = destMethodAccess.getIndex("set" + upperFirstLetter(property));
                destMethodAccess.invoke(dest, destIdx, srcMethodAccess.invoke(src, srcIdx));
            }
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    /**
     * 复制字段
     *
     * @param src
     * @param dest
     * @param keyMap key为源字段，value为目标字段
     */
    public static void copyProperties(Object src, Object dest, Map<String, String> keyMap) {
        Class srcClass = src.getClass();
        Class destClass = dest.getClass();
        for (Map.Entry<String, String> entry : keyMap.entrySet()) {
            try {
                Method srcMethod = srcClass.getMethod("get" + upperFirstLetter(entry.getKey()));
                Class retType = srcMethod.getReturnType();
                Method destMethod = destClass.getMethod("set" + upperFirstLetter(entry.getValue()), retType);
                destMethod.invoke(dest, srcMethod.invoke(src, null));
            } catch (Exception e) {
                throw new RuntimeException("could not copy the bean ,srcProperty:" + entry.getKey() + "; destProperty:" + entry.getValue());
            }
        }
    }

    public static Map<String, Object> copyPropertiesAsMap(Object src, String... properties) {
        Class srcClass = src.getClass();
        Map<String, Object> map = new HashMap<>();
        for (String property : properties) {
            try {
                Method srcMethod = srcClass.getMethod("get" + upperFirstLetter(property));
                map.put(property, srcMethod.invoke(src, null));
            } catch (Exception e) {
                throw new RuntimeException("could not copy the bean ,property:" + property);
            }
        }
        return map;
    }

    public static boolean equalsProperties(Object src, Object dest, String... properties) {
        Class srcClass = src.getClass();
        Class destClass = dest.getClass();
        for (String property : properties) {
            try {
                Method srcMethod = srcClass.getMethod("get" + upperFirstLetter(property));
                Object srcRet = srcMethod.invoke(src, null);
                Method destMethod = destClass.getMethod("get" + upperFirstLetter(property));
                Object destRet = destMethod.invoke(dest, null);
                if ((srcRet == null && destRet != null) || (srcRet != null && (!srcRet.equals(destRet)))) {
                    return false;
                }
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException("could not compare the bean ,property:" + property);
            }
        }
        return true;
    }

    /**
     * 根据字段判断object是否属于group，若group的属性为null,则认为该字段包含所有值
     *
     * @param group
     * @param object
     * @param properties
     * @return
     */
    public static boolean matchProperties(Object group, Object object, String... properties) {
        Class srcClass = group.getClass();
        Class destClass = object.getClass();
        for (String property : properties) {
            try {
                Method srcMethod = srcClass.getMethod("get" + upperFirstLetter(property));
                Object srcRet = srcMethod.invoke(group, null);
                Method destMethod = destClass.getMethod("get" + upperFirstLetter(property));
                Object destRet = destMethod.invoke(object, null);
                if (srcRet != null && (!srcRet.equals(destRet))) {
                    return false;
                }
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException("could not compare the bean ,property:" + property);
            }
        }
        return true;
    }

    public static String upperFirstLetter(String str) {
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }

    public static String lowerFirstLetter(String str) {
        return Character.toLowerCase(str.charAt(0)) + str.substring(1);
    }

    public static <T> T parse(Map<String, Object> map, Class<T> clazz) throws IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
        T object = clazz.newInstance();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            Method method = clazz.getMethod("set" + upperFirstLetter(entry.getKey()), entry.getValue().getClass());
            method.invoke(object, entry.getValue());
        }
        return object;
    }

    public static <T> T parseAndIgnoreError(Map<String, Object> map, Class<T> clazz) {
        T object;
        try {
            object = clazz.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            return null;
        }
        Method[] methods = clazz.getMethods();
        for (Method method : methods) {
            if (!method.getName().startsWith("set")) {
                continue;
            }
            String key = lowerFirstLetter(method.getName().substring(3));
            Class[] paramters = method.getParameterTypes();
            if (paramters.length != 1) {
                continue;
            }
            Object value = map.get(key);
            if (value == null) {
                continue;
            }
            try {
                if (paramters[0] == Long.class) {
                    method.invoke(object, Long.valueOf(value.toString()));
                } else if (paramters[0] == Integer.class) {
                    method.invoke(object, Integer.valueOf(value.toString()));
                } else if (paramters[0] == Short.class) {
                    method.invoke(object, Short.valueOf(value.toString()));
                } else if (paramters[0] == Double.class) {
                    method.invoke(object, Double.valueOf(value.toString()));
                } else if (paramters[0] == Float.class) {
                    method.invoke(object, Float.valueOf(value.toString()));
                } else {
                    method.invoke(object, value);
                }
            } catch (IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
        }
        return object;
    }
}
