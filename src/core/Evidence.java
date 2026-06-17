package core;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class Evidence {
    private final String name;
    private final String description;
    private final BufferedImage image;

    public Evidence(String name, String description, String imageFile) {
        this.name = name;
        this.description = description;
        BufferedImage loaded = null;
        try {
            loaded = ImageIO.read(new File("assets/sprites/" + imageFile));
        } catch (IOException e) {
            System.err.println("Could not load evidence image: " + imageFile);
        }
        this.image = loaded;
    }
    public String getName() {
        return name;
    }
    public String getDescription() {
        return description;
    }
    public BufferedImage getImage() {
        return image;
        //might return null if theres no image
    }
}