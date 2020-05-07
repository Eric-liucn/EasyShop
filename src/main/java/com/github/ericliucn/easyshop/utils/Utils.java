package com.github.ericliucn.easyshop.utils;

import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.serializer.TextSerializers;

public class Utils {

    public static Text strFormat(String string){
        return TextSerializers.FORMATTING_CODE.deserialize(string);
    }

}
