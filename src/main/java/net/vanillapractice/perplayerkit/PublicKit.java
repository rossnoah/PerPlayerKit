package net.vanillapractice.perplayerkit;

import org.bukkit.Material;

public class PublicKit{
    public String id;
    public String name;
    public Material icon;

    public PublicKit(String id,String name, Material icon){
        this.id = id;
        this.name = name;
        this.icon = icon;
    }
}