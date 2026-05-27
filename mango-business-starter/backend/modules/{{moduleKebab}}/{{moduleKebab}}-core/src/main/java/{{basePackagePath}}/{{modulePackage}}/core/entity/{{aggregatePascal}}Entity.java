package {{basePackage}}.{{modulePackage}}.core.entity;

/**
 * {{aggregatePascal}}持久化实体。
 */
public class {{aggregatePascal}}Entity {

    private String id;

    private String name;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
