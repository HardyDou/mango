package io.mango.infra.fileproc.render.command;

import java.util.ArrayList;
import java.util.List;

/**
 * 渲染变量定义。
 *
 * @param name 变量名。
 * @param type 变量类型。
 * @param children 子变量定义。
 */
public record RenderVariableDefinition(String name, String type, List<RenderVariableDefinition> children) {

    public RenderVariableDefinition {
        children = children == null ? List.of() : List.copyOf(children);
    }

    public static RenderVariableDefinition of(String name, String type) {
        return new RenderVariableDefinition(name, type, List.of());
    }

    public static RenderVariableDefinition of(String name, String type, List<RenderVariableDefinition> children) {
        return new RenderVariableDefinition(name, type, children);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private String name;

        private String type;

        private final List<RenderVariableDefinition> children = new ArrayList<>();

        private Builder() {
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder type(String type) {
            this.type = type;
            return this;
        }

        public Builder child(RenderVariableDefinition child) {
            if (child != null) {
                this.children.add(child);
            }
            return this;
        }

        public Builder children(List<RenderVariableDefinition> children) {
            if (children != null) {
                children.forEach(this::child);
            }
            return this;
        }

        public RenderVariableDefinition build() {
            return new RenderVariableDefinition(name, type, children);
        }
    }
}
