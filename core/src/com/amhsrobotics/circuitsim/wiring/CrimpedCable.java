package com.amhsrobotics.circuitsim.wiring;

import com.amhsrobotics.circuitsim.gui.CircuitGUIManager;
import com.amhsrobotics.circuitsim.utility.DeviceUtil;
import com.amhsrobotics.circuitsim.utility.Tools;
import com.amhsrobotics.circuitsim.utility.camera.ClippedCameraController;
import com.amhsrobotics.circuitsim.utility.input.Tuple;
import com.amhsrobotics.circuitsim.utility.scene.SnapGrid;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import me.rohanbansal.ricochet.tools.ModifiedShapeRenderer;

import java.util.ArrayList;


public class CrimpedCable extends Cable {

    public CrimpedCable(int gauge) {
        super(new Vector2(0, 0), -1);
        disableEnd = true;
        this.gauge = gauge;
    }

    public void update(ModifiedShapeRenderer renderer, ClippedCameraController camera) {

        limit = ((1/gauge)*100 + 3f)/2;

        renderer.setProjectionMatrix(camera.getCamera().combined);
        renderer.begin(ShapeRenderer.ShapeType.Filled);

        // LOGIC
        // ---------------------------------------------------------------------
        if(nodeChanged) {
            nodeChanged = false;
        }
        disableBegin = connection1 != null;
        disableEnd = true;
        appendingFromBegin = false;
        appendingFromEnd = false;
        // ---------------------------------------------------------------------



        // DRAW CABLE
        // ---------------------------------------------------------------------
        renderer.setColor(color);
        for(int i = 0; i < coordinates.size() - 1; ++i) {
            if(CableManager.currentCable != null) {
                if(CableManager.currentCable == this) {
                    // draw cable selected
                    renderer.setColor(new Color(217/255f, 233/255f, 217/255f, 1));
                    renderer.rectLine(coordinates.get(i), coordinates.get(i + 1), (1/gauge)*140);
                }
            }
            if(hoveringMouse(camera)) {
                // draw hovering on cable
                renderer.setColor(new Color(217 / 255f, 233 / 255f, 217 / 255f, 1));
                renderer.rectLine(coordinates.get(i), coordinates.get(i + 1), (1/gauge)*140);
            }
            // draw actual cable
            renderer.setColor(color);
            renderer.rectLine(coordinates.get(i), coordinates.get(i + 1), (1/gauge)*100);

            renderer.circle(coordinates.get(i).x, coordinates.get(i).y, limit);
        }
        renderer.circle(coordinates.get(coordinates.size() - 1).x, coordinates.get(coordinates.size() - 1).y, limit);
        // ---------------------------------------------------------------------



        // CABLE SELECTED MECHANICS
        // ---------------------------------------------------------------------

        if(CableManager.currentCable == this) {

            CableManager.moveToFront(this);

            Vector2 vec2 = Tools.mouseScreenToWorld(camera);

            if (Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)) {
                SnapGrid.calculateSnap(vec2);
            }

            // IF MERGING WIRES
            Tuple<Cable, Integer> secondCable = CableManager.wireHoveringWire(camera, this);
            if (secondCable != null && Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
                CableManager.mergeCables(this, secondCable.x, secondCable.y == 1, appendingFromBegin);
            } else {

                // UNSELECT
                if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
                    CableManager.currentCable = null;
                    appendingFromBegin = false;
                    CircuitGUIManager.propertiesBox.hide();
                }

                // DRAW NODES IF SELECTED
                drawNodes(renderer, camera, Color.SALMON);

            }

        }
        // ---------------------------------------------------------------------

        // HOVERING OVER CABLE
        // ---------------------------------------------------------------------

        if(hoveringMouse(camera)) {
            drawNodes(renderer, camera, Color.SALMON);
            checkForClick(camera);
        }

        // ---------------------------------------------------------------------

        renderer.end();
    }

    @Override
    public void populateProperties() {
        CircuitGUIManager.propertiesBox.clearTable();
        CircuitGUIManager.propertiesBox.addElement(new Label("Crimped Cable", CircuitGUIManager.propertiesBox.LABEL), true, 2);
        CircuitGUIManager.propertiesBox.addElement(new Label("Color", CircuitGUIManager.propertiesBox.LABEL_SMALL), true, 1);
        final TextButton cb = new TextButton(DeviceUtil.getKeyByValue(DeviceUtil.COLORS, this.color), CircuitGUIManager.propertiesBox.TBUTTON);
        CircuitGUIManager.propertiesBox.addElement(cb, false, 1);

        cb.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                ArrayList<String> keys = new ArrayList<>(DeviceUtil.COLORS.keySet());
                for(String str : keys) {
                    if(str.contentEquals(cb.getText())) {
                        if(keys.indexOf(str) == keys.size() - 1) {
                            cb.setText(keys.get(0));
                            color = DeviceUtil.COLORS.get(keys.get(0));
                        } else {
                            cb.setText(keys.get(keys.indexOf(str) + 1));
                            color = DeviceUtil.COLORS.get(keys.get(keys.indexOf(str) + 1));
                        }
                        break;
                    }
                }
            }
        });
    }
}