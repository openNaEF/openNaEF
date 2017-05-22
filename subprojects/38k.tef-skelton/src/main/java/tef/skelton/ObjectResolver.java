package tef.skelton;

import tef.skelton.fqn.Fqn;
import tef.skelton.fqn.FqnSyntax;
import tef.skelton.fqn.Term;

import java.util.ArrayList;
import java.util.List;

import static tef.skelton.SystemPropertyIds.*;

public class ObjectResolver {

    public static <T> T resolve(Class<? extends T> type, Model context, VariableHolder variableHolder, String fqnStr)
        throws ResolveException 
    {
        String fqnPrimaryDelimiter = getProperty(FQN_PRIMARY_DELIMITER);
        String fqnSecondaryDelimiter = getProperty(FQN_SECONDARY_DELIMITER);
        String fqnTertiaryDelimiter = getProperty(FQN_TERTIARY_DELIMITER);
        String fqnLeftBracket = getProperty(FQN_LEFT_BRACKET);
        String fqnRightBracket = getProperty(FQN_RIGHT_BRACKET);
        if (fqnPrimaryDelimiter == null) {
            throw new ResolveException("fqn primary delimiter を指定してください.");
        }
        if (fqnSecondaryDelimiter == null) {
            throw new ResolveException("fqn secondary delimiter を指定してください.");
        }
        if (fqnLeftBracket == null) {
            throw new ResolveException("fqn left bracket を指定してください.");
        }
        if (fqnRightBracket == null) {
            throw new ResolveException("fqn right bracket を指定してください.");
        }

        Fqn fqn;
        try {
            fqn = FqnSyntax.parse(fqnPrimaryDelimiter, fqnSecondaryDelimiter, fqnTertiaryDelimiter, fqnStr);
        } catch (lib38k.parser.ParseException pe) {
            throw new ResolveException(pe.getMessage());
        }
        if (fqn == null) {
            throw new RuntimeException("解析に失敗しました: " + fqnStr);
        }

        Object object;
        if (fqn instanceof Fqn.Single) {
            object = resolveAsSingleFqn(context, variableHolder, (Fqn.Single) fqn);
        } else if (fqn instanceof Fqn.Clustering) {
            object = resolveAsClusteringFqn(context, variableHolder, (Fqn.Clustering) fqn);
        } else {
            throw new RuntimeException(fqn.getClass().getName());
        }

        if (! type.isInstance(object)) {
            throw new ResolveException(
                "指定された型 "
                + SkeltonTefService.instance().uiTypeNames().getName(type) + " に対して "
                + SkeltonTefService.instance().uiTypeNames().getName(object.getClass())
                + " が見つかりました.");
        }

        return type.cast(object);
    }

    private static Object resolveAsSingleFqn(Model context, VariableHolder variableHolder, Fqn.Single fqn)
        throws ResolveException
    {
        Model object = null;
        for (Term term : fqn.terms()) {
            if (term.typeName != null && term.typeName.equals("$")) {
                if (variableHolder == null) {
                    throw new ResolveException("変数は使用できません.");
                }

                String variableName = term.objectName;
                Object variableValue = variableHolder.getVariable(variableName);
                if (variableValue == null) {
                    throw new ResolveException("変数に値が設定されていません: " + variableName);
                }
                if (! (variableValue instanceof tef.MVO)
                    || ! (variableValue instanceof Model))
                {
                    throw new ResolveException("変数の値が不正です: " + variableValue.getClass().getName());
                }

                object = (Model) variableValue;
            } else {
                Resolver.SingleNameResolver<?, ?> resolver;
                if (term.typeName == null) { 
                    resolver = getInferredResolver(context, term.objectName);
                    if (resolver == null) {
                        throw new ResolveException("オブジェクトが見つかりません: 型推定, '" + term.objectName + "'");
                    }
                } else {
                    Resolver<?> r = SkeltonTefService.instance().getResolver(term.typeName);
                    if (r == null) {
                        throw new ResolveException("リゾルバの定義がありません: " + term.typeName);
                    }

                    if (! (r instanceof Resolver.SingleNameResolver<?, ?>)) {
                        throw new ResolveException("リゾルバの型が不適合です: " + term.typeName);
                    }
                    resolver = (Resolver.SingleNameResolver<?, ?>) r;
                }

                object = (Model) resolver.resolve(context, term.objectName);
                if (object == null) {
                    throw new ResolveException(
                        "オブジェクトが見つかりません: " + resolver.getName() + ", " + term.objectName);
                }
            }

            context = object;
        }
        return object;
    }

    private static Object resolveAsClusteringFqn(
        Model context, VariableHolder variableHolder, Fqn.Clustering fqn)
        throws ResolveException
    {
        if (fqn.typeName() == null || fqn.typeName().equals("")) {
            throw new ResolveException("集合型には型名が必要です.");
        }

        Resolver<?> r = SkeltonTefService.instance().getResolver(fqn.typeName());
        if (r == null) {
            throw new ResolveException("リゾルバの定義がありません: " + fqn.typeName());
        }
        if (! (r instanceof Resolver.ClusteringResolver<?, ?>)) {
            throw new ResolveException("リゾルバの型が不適合です: " + fqn.typeName());
        }

        List<Object> elements = new ArrayList<Object>();
        for (Fqn.Single elementFqn : fqn.elements()) {
            elements.add(resolveAsSingleFqn(context, variableHolder, elementFqn));
        }

        Resolver.ClusteringResolver<?, ?> resolver = (Resolver.ClusteringResolver<?, ?>) r;
        return resolver.resolve(elements);
    }

    private static Resolver.SingleNameResolver<?, ?> getInferredResolver(Model context, String arg)
        throws ResolveException
    {
        Resolver.SingleNameResolver<?, ?> result = null;
        Object lastResolved = null;

        for (Resolver r : SkeltonTefService.instance().getResolvers()) {
            if (! (r instanceof Resolver.SingleNameResolver<?, ?>)) {
                continue;
            }

            Resolver.SingleNameResolver<?, ?> resolver = (Resolver.SingleNameResolver<?, ?>) r;
            if (resolver.getRequiredContextType() == null) {
                continue;
            }

            Object resolved;
            try {
                resolved = resolver.resolve(context, arg);
            } catch (ResolveException re) {
                continue;
            }

            if (resolved != null) {
                if (result == null) {
                    result = resolver;
                    lastResolved = resolved;
                } else if (lastResolved != resolved) {
                    throw new ResolveException(
                        "指定名を持つオブジェクトが複数あるため一意に決定できません" + "(型指定が必要です): "+ arg);
                }
            }
        }
        if (result != null) {
            return result;
        }

        for (Resolver r : SkeltonTefService.instance().getResolvers()) {
            if (! (r instanceof Resolver.SingleNameResolver<?, ?>)) {
                continue;
            }

            Resolver.SingleNameResolver<?, ?> resolver = (Resolver.SingleNameResolver<?, ?>) r;
            if (resolver.getRequiredContextType() != null) {
                continue;
            }

            Object resolved;
            try {
                resolved = resolver.resolve(context, arg);
            } catch (ResolveException re) {
                continue;
            }

            if (resolved != null) {
                if (result == null) {
                    result = resolver;
                    lastResolved = resolved;
                } else if (lastResolved != resolved) {
                    throw new ResolveException(
                        "指定名を持つオブジェクトが複数あるため一意に決定できません" + "(型指定が必要です): "+ arg);
                }
            }
        }

        return result;
    }

    private static String getProperty(String key) {
        return tef.TefService.instance().getSystemProperties().get(key);
    }
}
