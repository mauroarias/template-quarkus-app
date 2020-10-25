package datadog.lib.datadog.trace.common.writer;

import datadog.trace.api.DDId;
import datadog.lib.datadog.trace.core.DDSpan;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class TraceStructureWriter implements Writer {
    private final PrintStream out;

    public TraceStructureWriter() {
        this("");
    }

    public TraceStructureWriter(String outputFile) {
        try {
            this.out = !outputFile.isEmpty() && !outputFile.equals(":") ? new PrintStream(new FileOutputStream(new File(outputFile.replace(":", "")))) : System.err;
        } catch (IOException var3) {
            throw new RuntimeException("Failed to create trace structure writer from " + outputFile, var3);
        }
    }

    public void write(List<DDSpan> trace) {
        if (trace.isEmpty()) {
            this.out.println("[]");
        } else {
            DDId traceId = ((DDSpan)trace.get(0)).getTraceId();
            DDId rootSpanId = ((DDSpan)trace.get(0)).getSpanId();
            Map<DDId, Node> nodesById = new HashMap();

            Iterator var5;
            DDSpan span;
            for(var5 = trace.iterator(); var5.hasNext(); nodesById.put(span.getSpanId(), new Node(span))) {
                span = (DDSpan)var5.next();
                if (DDId.ZERO.equals(span.getParentId())) {
                    rootSpanId = span.getSpanId();
                }
            }

            var5 = trace.iterator();

            while(var5.hasNext()) {
                span = (DDSpan)var5.next();
                if (!traceId.equals(span.getTraceId())) {
                    this.out.println("ERROR: Trace " + traceId + " has broken trace link at " + span.getSpanId() + "(" + span.getOperationName() + ")->" + span.getTraceId());
                    return;
                }

                if (!rootSpanId.equals(span.getSpanId())) {
                    Node parent = (Node)nodesById.get(span.getParentId());
                    if (null == parent) {
                        this.out.println("ERROR: Trace " + traceId + " has broken link at " + span.getSpanId() + "(" + span.getOperationName() + ")->" + span.getParentId());
                        return;
                    }

                    parent.addChild((Node)nodesById.get(span.getSpanId()));
                }
            }

            this.out.println(nodesById.get(rootSpanId));
        }

    }

    public void start() {
    }

    public boolean flush() {
        this.out.flush();
        return true;
    }

    public void close() {
        if (this.out != System.err) {
            this.out.close();
        }

    }

    public void incrementTraceCount() {
    }

    private static final class Node {
        private final CharSequence operationName;
        private final List<Node> children;

        private Node(DDSpan span) {
            this.children = new ArrayList();
            this.operationName = span.getOperationName();
        }

        public void addChild(Node child) {
            this.children.add(child);
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("[").append(this.operationName);
            Iterator var2 = this.children.iterator();

            while(var2.hasNext()) {
                Node node = (Node)var2.next();
                sb.append(node);
            }

            return sb.append("]").toString();
        }
    }
}
