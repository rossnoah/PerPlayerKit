# PerPlayerKit API

The PerPlayerKit API is a simple Java API that allows developers to interact with the plugin. The API is **NOT** stable and will possibly change in the future.

### Example Usage:

Add plugin jar to `./lib` folder in your project.

Add to pom.xml:

```
<dependency>
    <groupId>com.local</groupId>
    <artifactId>PerPlayerKit</artifactId>
    <version>local</version>
    <scope>system</scope>
    <systemPath>${project.basedir}/lib/PerPlayerKit-1.1.jar</systemPath>
</dependency>
```

```java
import dev.noah.perplayerkit.API;
import dev.noah.perplayerkit.PublicKit;
// other imports etc

public class ExamplePlugin extends JavaPlugin {

    public void onEnable() {
        // On enable code...
    }


// Give a player a public kit when they join the server
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        // Get the API instance
        API api = API.getInstance();

        // Get a list of all public kits
        List<PublicKit> publicKits = api.getPublicKits();

        // Load a public kit
        api.loadPublicKit(e.getPlayer(), publicKits.get(0));
    }

}
```
