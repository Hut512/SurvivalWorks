package de.survivalworkers.core.client.engine;

/**
 * Used to identify a file path or object of any sorts using the Namespace(The Provider) and the Identifier itself allowing multiple Registration of Multiple Providers (Providers are in this case Games,Plugins,Mods)
 */
public class Identifier {
    private String nameSpace;
    private String id;

    public Identifier(String nameSpace,String id){
        this.nameSpace = nameSpace;
        this.id = id;
        validate(nameSpace);
        validate(id);
    }

    public Identifier(String id){
        this("Main.NAME.toLowerCase(Locale.ROOT)",id);
    }

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof Identifier))return false;
        return nameSpace.equals(((Identifier) obj).nameSpace) && id.equals(((Identifier) obj).id);
    }

    @Override
    public String toString() {
        return nameSpace + ":" + id;
    }

    /**
     * @param arg The String to be validated, Exception thrown when letter is not Lower case or one of [ , \ , ] , ^ , _ , { , | , }
     */
    private void validate(String arg){
        for (char c : arg.toCharArray()) {
            if(!(c > 90 && c < 126))throw new IllegalArgumentException("Invalid Identifier name in " + arg + " at " + c);
        }
    }
}
