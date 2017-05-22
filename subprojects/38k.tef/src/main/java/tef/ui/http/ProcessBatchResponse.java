package tef.ui.http;

import lib38k.net.httpd.Html;
import lib38k.net.httpd.HttpException;
import lib38k.net.httpd.HttpResponse;
import lib38k.net.httpd.HttpResponseContents;
import tef.ui.shell.InternalShellInvocation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class ProcessBatchResponse extends TefHttpResponse {

    public ProcessBatchResponse() {
    }

    public HttpResponseContents getContents() throws HttpException {
        String[] commandlines;
        try {
            commandlines = getCommandlines().toArray(new String[0]);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }

        InternalShellInvocation shellInvocation = new InternalShellInvocation();
        try {
            for (int i = 0; i < commandlines.length; i++) {
                String commandline = commandlines[i];
                try {
                    shellInvocation.processCommandline(commandline);
                } catch (InternalShellInvocation.InvocationException ie) {
                    String messagePrefix = "line" + Integer.toString(i + 1) + ": ";
                    if (ie.getError() != null) {
                        throw new RuntimeException
                                (messagePrefix + ie.getMessage(), ie.getError());
                    } else {
                        throw new HttpResponse.ResponseException
                                (messagePrefix + ie.getMessage());
                    }
                }
            }
        } finally {
            try {
                shellInvocation.close();
            } catch (IOException ioe) {
                throw new RuntimeException(ioe);
            }
        }

        return new Html(UTF8);
    }

    private List<String> getCommandlines() throws IOException {
        List<String> result = new ArrayList<String>();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(getRequest().getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                result.add(line);
            }
            return result;
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
    }
}
