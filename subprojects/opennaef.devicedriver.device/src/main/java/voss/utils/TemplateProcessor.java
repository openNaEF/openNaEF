package voss.utils;

import javax.script.Compilable;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

public final class TemplateProcessor {
    private TemplateProcessor() {

    }

    public static void compile(final String mimeType, final String body)
            throws ScriptException {
        ScriptEngineManager engineMgr = new ScriptEngineManager();
        Compilable engine = (Compilable) engineMgr.getEngineByMimeType(mimeType);
        engine.compile(body);
    }
}