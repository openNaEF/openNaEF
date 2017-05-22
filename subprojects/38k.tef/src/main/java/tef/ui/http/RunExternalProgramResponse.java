package tef.ui.http;

import lib38k.net.httpd.HttpException;
import lib38k.net.httpd.HttpResponseContents;
import tef.ExternalProgramLoader;
import tef.TransactionContext;

import java.lang.reflect.InvocationTargetException;

public class RunExternalProgramResponse extends TefHttpResponse {

    private static final String ARG_NAME_EXTERNAL_PROGRAM_NAME = "class";
    private static final String EXTERNAL_PROGRAM_METHOD_NAME = "run";

    public RunExternalProgramResponse() {
    }

    public HttpResponseContents getContents() throws HttpException {
        HttpExtProgram externalProgram;
        try {
            externalProgram = getExternalProgram((TefHttpRequest) getRequest());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        HttpResponseContents methodInvocationResult;
        try {
            methodInvocationResult = invokeMethod(externalProgram);
        } catch (HttpExtProgram.HttpExtProgramException hepe) {
            throw new ResponseException("error", hepe.getMessage());
        }

        return methodInvocationResult;
    }

    private HttpExtProgram getExternalProgram(TefHttpRequest request)
            throws HttpException,
            NoSuchMethodException,
            InstantiationException,
            IllegalAccessException,
            InvocationTargetException {
        String externalProgramName
                = request.getParameter(ARG_NAME_EXTERNAL_PROGRAM_NAME);

        if (externalProgramName == null) {
            throw new ResponseException
                    ("error",
                            "<font color=red>[error]</font> "
                                    + "invalid argument: no class name.");
        }

        try {
            ExternalProgramLoader programLoader
                    = new ExternalProgramLoader
                    (externalProgramName, EXTERNAL_PROGRAM_METHOD_NAME);

            HttpExtProgram result
                    = (HttpExtProgram) programLoader.getExternalProgramObject();

            result.request = request;
            result.response = RunExternalProgramResponse.this;

            return result;
        } catch (ClassNotFoundException cnfe) {
            throw new ResponseException
                    ("error",
                            "<font color=red>[error]</font> "
                                    + "program not found: " + externalProgramName);
        } catch (ClassCastException cce) {
            throw new ResponseException
                    ("error",
                            "<font color=red>[error]</font> "
                                    + "the program is not an HttpExtProgram.");
        }
    }

    private HttpResponseContents invokeMethod(HttpExtProgram externalProgram)
            throws HttpExtProgram.HttpExtProgramException {
        try {
            return externalProgram.run();
        } finally {
            TransactionContext.close();
        }
    }
}
