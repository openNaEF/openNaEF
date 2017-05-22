package tef.skelton.shell;

import tef.skelton.FormatException;
import tef.skelton.Model;
import tef.skelton.ObjectResolver;
import tef.skelton.ResolveException;
import tef.skelton.SkeltonTefService;
import tef.skelton.UiTypeName;
import tef.skelton.ValueResolver;
import tef.ui.shell.ShellCommand;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

public abstract class SkeltonShellCommand extends ShellCommand {

    @Override protected SkeltonShellSession getSession() {
        return (SkeltonShellSession) super.getSession();
    }

    protected void setContext(Model model, String prompt) {
        getSession().setContext(model, prompt);
    }

    protected Model getContext() {
        return getSession().getContext();
    }

    protected <T> T contextAs(Class<T> klass, String caption)
        throws ShellCommandException 
    {
        Object context = getContext();
        if (context == null) {
            throw new ShellCommandException("コンテキストが指定されていません.");
        }

        if (! klass.isInstance(context)) {
            throw new ShellCommandException("コンテキストが" + caption + "ではありません.");
        }

        return klass.cast(context);
    }

    protected static int parseInt(String str) throws ShellCommandException {
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException nfe) {
            throw new ShellCommandException("数値形式に適合しません: " + str);
        }
    }

    protected static long parseLong(String str) throws ShellCommandException {
        try {
            return Long.parseLong(str);
        } catch (NumberFormatException nfe) {
            throw new ShellCommandException("数値形式に適合しません: " + str);
        }
    }

    protected UiTypeName resolveTypeName(String typeName) throws ShellCommandException {
        UiTypeName result = SkeltonTefService.instance().uiTypeNames().getByName(typeName);
        if (result == null) {
            throw new ShellCommandException("型名を確認してください: " + typeName);
        }
        return result;
    }

    protected <T> UiTypeName resolveTypeNameAs(Class<T> expectedType, String typeName)
        throws ShellCommandException
    {
        UiTypeName type = resolveTypeName(typeName);
        if (! expectedType.isAssignableFrom(type.type())) {
            throw new ShellCommandException(
                "指定された型は " + SkeltonTefService.instance().uiTypeNames().getName(expectedType) + " ではありません.");
        }
        return type;
    }

    protected <T extends Enum<T>> T resolveEnum(String enumName, Class<T> enumClass, String resolvee)
        throws ShellCommandException
    {
        try {
            return ValueResolver.resolveEnum(enumClass, resolvee, true);
        } catch (FormatException fe) {
            throw new ShellCommandException(enumName + " を確認してください.");
        }
    }

    protected <T> T resolve(Class<T> type, String name)
        throws ShellCommandException
    {
        try {
            return ObjectResolver.<T>resolve(type, getContext(), getSession(), name);
        } catch (ResolveException re) {
            throw new ShellCommandException(re.getMessage());
        }
    }

    protected <T> List<T> resolveQualifiedNames(Class<T> type, List<String> qualifiedNames)
        throws ShellCommandException
    {
        List<T> result = new ArrayList<T>();
        for (String name : qualifiedNames) {
            result.add(resolve(type, name));
        }
        return result;
    }

    protected static void validateTypeInstantiatable(Class<?> klass)
        throws ShellCommandException
    {
        int modifiers = klass.getModifiers();
        if (Modifier.isAbstract(modifiers) || Modifier.isInterface(modifiers)) {
            throw new ShellCommandException("抽象型の生成はできません.");
        }
    }
}
