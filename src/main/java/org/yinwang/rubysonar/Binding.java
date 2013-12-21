package org.yinwang.rubysonar;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yinwang.rubysonar.ast.Class;
import org.yinwang.rubysonar.ast.*;
import org.yinwang.rubysonar.types.ModuleType;
import org.yinwang.rubysonar.types.Type;

import java.util.LinkedHashSet;
import java.util.Set;


public class Binding implements Comparable<Object> {

    public enum Kind {
        ATTRIBUTE,    // attr accessed with "." on some other object
        CLASS,        // class definition
        CONSTRUCTOR,  // __init__ functions in classes
        FUNCTION,     // plain function
        METHOD,       // static or instance method
        MODULE,       // file
        PARAMETER,    // function param
        SCOPE,        // top-level variable ("scope" means we assume it can have attrs)
        VARIABLE      // local variable
    }


    private boolean isStatic = false;         // static fields/methods
    private boolean isSynthetic = false;      // auto-generated bindings
    private boolean isReadonly = false;       // non-writable attributes
    private boolean isDeprecated = false;     // documented as deprecated
    private boolean isBuiltin = false;        // not from a source file

    @NotNull
    private String name;     // unqualified name
    @NotNull
    public Node node;
    @NotNull
    private String qname;    // qualified name
    private Type type;       // inferred type
    public Kind kind;        // name usage context

    private Set<Node> refs;

    // fields from Def
    public int start = -1;
    public int end = -1;
    public int bodyStart = -1;
    public int bodyEnd = -1;

    @Nullable
    private String fileOrUrl;


    public Binding(@NotNull String id, @NotNull Node node, @NotNull Type type, @NotNull Kind kind) {
        this.name = id;
        this.qname = type.getTable().getPath();
        this.type = type;
        this.kind = kind;
        this.node = node;

        if (node instanceof Url) {
            String url = ((Url) node).getURL();
            if (url.startsWith("file://")) {
                fileOrUrl = url.substring("file://".length());
            } else {
                fileOrUrl = url;
            }
        } else {
            fileOrUrl = node.file;
            if (node instanceof Name) {
                name = node.asName().id;
            }
        }

        initLocationInfo(node);
        Analyzer.self.registerBinding(this);
    }


    private void initLocationInfo(Node node) {
        start = node.start;
        end = node.end;

        Node parent = node.parent;
        if ((parent instanceof Function && ((Function) parent).name == node) ||
                (parent instanceof Class && ((Class) parent).locator == node))
        {
            bodyStart = parent.start;
            bodyEnd = parent.end;
        } else if (node instanceof Module) {
            name = ((Module) node).name.id;
            start = 0;
            end = 0;
            bodyStart = node.start;
            bodyEnd = node.end;
        } else {
            bodyStart = node.start;
            bodyEnd = node.end;
        }
    }


    @NotNull
    public String getName() {
        return name;
    }


    public void setQname(@NotNull String qname) {
        this.qname = qname;
    }


    @NotNull
    public String getQname() {
        return qname;
    }


    public void addRef(Node ref) {
        getRefs().add(ref);
    }


    public void setType(Type type) {
        this.type = type;
    }


    public Type getType() {
        return type;
    }


    public void setKind(Kind kind) {
        this.kind = kind;
    }


    public Kind getKind() {
        return kind;
    }


    public void markStatic() {
        isStatic = true;
    }


    public boolean isStatic() {
        return isStatic;
    }


    public void markSynthetic() {
        isSynthetic = true;
    }


    public boolean isSynthetic() {
        return isSynthetic;
    }


    public void markReadOnly() {
        isReadonly = true;
    }


    public boolean isBuiltin() {
        return isBuiltin;
    }


    public Set<Node> getRefs() {
        if (refs == null) {
            refs = new LinkedHashSet<>(1);
        }
        return refs;
    }


    @NotNull
    public String getFirstFile() {
        Type bt = getType();
        if (bt instanceof ModuleType) {
            String file = bt.asModuleType().getFile();
            return file != null ? file : "<built-in module>";
        }

        String file = getFile();
        if (file != null) {
            return file;
        }

        return "<built-in binding>";
    }


    @Nullable
    public String getFile() {
        return isURL() ? null : fileOrUrl;
    }


    @Nullable
    public String getURL() {
        return isURL() ? fileOrUrl : null;
    }


    @Nullable
    public String getFileOrUrl() {
        return fileOrUrl;
    }


    public boolean isURL() {
        return fileOrUrl != null && fileOrUrl.startsWith("http://");
    }


    /**
     * Bindings can be sorted by their location for outlining purposes.
     */
    public int compareTo(@NotNull Object o) {
        return start - ((Binding) o).start;
    }


    @NotNull
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("<Binding:");
        sb.append(":qname=").append(qname);
        sb.append(":type=").append(type);
        sb.append(":kind=").append(kind);
        sb.append(":node=").append(node);
        sb.append(":refs=");
        if (getRefs().size() > 10) {
            sb.append("[");
            sb.append(refs.iterator().next());
            sb.append(", ...(");
            sb.append(refs.size() - 1);
            sb.append(" more)]");
        } else {
            sb.append(refs);
        }
        sb.append(">");
        return sb.toString();
    }


    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Binding)) {
            return false;
        } else {
            Binding b = (Binding) obj;
            return (start == b.start
                    && end == b.end
                    && ((fileOrUrl == null && b.fileOrUrl == null)
                    || (fileOrUrl != null && b.fileOrUrl != null &&
                    fileOrUrl.equals(b.fileOrUrl))));
        }
    }


    @Override
    public int hashCode() {
        return ("" + fileOrUrl + start).hashCode();
    }

}
