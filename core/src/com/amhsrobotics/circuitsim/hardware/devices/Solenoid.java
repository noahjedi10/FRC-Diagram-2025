package com.amhsrobotics.circuitsim.hardware.devices;

import com.amhsrobotics.circuitsim.hardware.Flippable;
import com.amhsrobotics.circuitsim.hardware.HardwareType;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.math.Vector2;
import org.json.simple.JSONArray;

public class Solenoid extends Flippable {

    public Solenoid(Vector2 position, HardwareType type, boolean... addCrimped) {
        super(position, type, addCrimped);


        for(JSONArray arr : pinDefs) {
            Sprite temp;
            if(connectors.size() == connNum) {
                break;
            }
            temp = new Sprite(new Texture(Gdx.files.internal("img/point.png")));
            temp.setCenter(position.x + (Long) arr.get(0), position.y + (Long) arr.get(1));
            temp.setSize((Long)pinSizeDefs.get(pinDefs.indexOf(arr)).get(0) /2f, (Long)pinSizeDefs.get(pinDefs.indexOf(arr)).get(1) /2f);
            connectors.add(temp);
        }

        initConnections();
        initEnds();
    }

    public Vector2 calculate(int port) {
        if(type == HardwareType.DOUBLESOLENOID) {
            if(port == 2 || port == 3) {
                return calculateDirection(3+cur, port);
            } else if (port == 4 || port == 5) {
                return calculateDirection(1+cur, port);
            } else if (port == 1) {
                return calculateDirection(3+cur, port, 100);
            } else {
                return calculateDirection(1+cur, port, 100);
            }
        } else {
            if(port == 0) {
                return calculateDirection(1+cur, port, 100);
            } else {
                return calculateDirection(3+cur, port, 100);
            }
        }
    }
}