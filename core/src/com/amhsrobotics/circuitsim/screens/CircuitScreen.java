package com.amhsrobotics.circuitsim.screens;

import com.amhsrobotics.circuitsim.Constants;
import com.amhsrobotics.circuitsim.gui.CircuitGUIManager;
import com.amhsrobotics.circuitsim.hardware.Hardware;
import com.amhsrobotics.circuitsim.hardware.HardwareManager;
import com.amhsrobotics.circuitsim.hardware.HardwareType;
import com.amhsrobotics.circuitsim.hardware.devices.*;
import com.amhsrobotics.circuitsim.utility.Tools;
import com.amhsrobotics.circuitsim.utility.camera.ClippedCameraController;
import com.amhsrobotics.circuitsim.utility.camera.Rumble;
import com.amhsrobotics.circuitsim.utility.input.InputManager;
import com.amhsrobotics.circuitsim.utility.scene.ModifiedStage;
import com.amhsrobotics.circuitsim.utility.scene.SnapGrid;
import com.amhsrobotics.circuitsim.wiring.CableManager;
import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.viewport.FitViewport;
import me.rohanbansal.ricochet.camera.CameraAction;
import me.rohanbansal.ricochet.tools.Actions;
import me.rohanbansal.ricochet.tools.ModifiedShapeRenderer;

import java.util.ArrayList;

public class CircuitScreen implements Screen {

    private final ModifiedStage stage;
    private final Game game;
    private final SpriteBatch batch;
    private final ModifiedShapeRenderer renderer;
    private final ModifiedShapeRenderer HUDrenderer;
    private ClippedCameraController camera;
    private static BitmapFont hoverFont = Tools.renderFont("font/Abel-Regular.ttf", 40, true);
    private static String drawString = "";
    private static Vector2 drawLoc = new Vector2();
    private static GlyphLayout layout = new GlyphLayout();

    private Hardware currentPlacingHardware;

    private CircuitGUIManager manager;

    static {
        hoverFont.setColor(Color.SALMON);
    }


    public CircuitScreen(final Game game) {


        this.game = game;
        this.batch = new SpriteBatch();
        this.renderer = new ModifiedShapeRenderer();
        this.HUDrenderer = new ModifiedShapeRenderer();

        camera = new ClippedCameraController(true);
        camera.getCamera().translate(Constants.WORLD_DIM.x / 2 - (Constants.WORLD_DIM.x / 2) % Constants.GRID_SIZE-3, Constants.WORLD_DIM.y / 2 - (Constants.WORLD_DIM.x / 2) % Constants.GRID_SIZE-2);
        camera.attachCameraSequence(new ArrayList<CameraAction>() {{
            add(Actions.zoomCameraTo(2f, 1f, Interpolation.exp10));
        }});

        stage = new ModifiedStage(new FitViewport(Gdx.graphics.getWidth(), Gdx.graphics.getHeight()), batch);
        manager = new CircuitGUIManager(stage, camera, game);

        InputMultiplexer plexer = new InputMultiplexer(stage, new InputManager() {
            @Override
            public boolean touchDragged(int screenX, int screenY, int pointer) {
                if(Constants.placing_object == null && !HardwareManager.movingObject && CableManager.currentCable == null && HardwareManager.currentHardware == null) {
                    float x = Gdx.input.getDeltaX();
                    float y = Gdx.input.getDeltaY();

                    camera.getCamera().translate(-x, y);
                }

                return super.touchDragged(screenX, screenY, pointer);
            }

            @Override
            public boolean scrolled(int amount) {
                if(Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)) {
                    camera.getCamera().translate(0, amount > 0 ? 45f : -45f);
                } else if(Gdx.input.isKeyPressed(Input.Keys.ALT_LEFT)) {
                    camera.getCamera().translate(amount > 0 ? 45f : -45f, 0);
                } else {
                    camera.getCamera().zoom *= amount > 0 ? 1.05f : 0.95f;
                    if(camera.getCamera().zoom > 3.55) {
                        camera.getCamera().zoom = 3.55f;
                    } else if(camera.getCamera().zoom < 0.2) {
                        camera.getCamera().zoom = 0.2f;
                    }
                }

                return super.scrolled(amount);
            }
        });
        Gdx.input.setInputProcessor(plexer);
    }

    @Override
    public void render(float delta) {

        camera.update();
        camera.calculateBounds();

        if (Rumble.getRumbleTimeLeft() > 0){
            Rumble.tick(Gdx.graphics.getDeltaTime());
            camera.getCamera().translate(Rumble.getPos());
        }

        renderer.setProjectionMatrix(camera.getCamera().combined);
        SnapGrid.renderGrid(renderer, new Color(0/255f, 0/255f, 30/255f, 1), Constants.WORLD_DIM, Constants.GRID_SIZE, 0);

        Vector2 vec2 = Tools.mouseScreenToWorld(camera);

        if(Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)) {
            SnapGrid.calculateSnap(vec2);
        }

        if(Constants.placing_object != null) {


            HUDrenderer.setColor(Color.RED);
            HUDrenderer.begin(ShapeRenderer.ShapeType.Filled);
            HUDrenderer.rectLine(0, 0, 0, Gdx.graphics.getHeight(), 6);
            HUDrenderer.rectLine(0, Gdx.graphics.getHeight(), Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), 6);
            HUDrenderer.rectLine(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), Gdx.graphics.getWidth(), 0, 6);
            HUDrenderer.rectLine(0, 0, Gdx.graphics.getWidth(), 0, 6);
            HUDrenderer.end();

            if(Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
                Constants.placing_object = null;
            }



            if(currentPlacingHardware != null && currentPlacingHardware.type == Constants.placing_object) {
                currentPlacingHardware.setPosition(vec2.x, vec2.y);
            } else {

                if (Constants.placing_object == HardwareType.WIRE) {
                    drawPlacing(vec2.x, vec2.y);
                } else if (Constants.placing_object == HardwareType.DOUBLESANDCRAB) {
                    currentPlacingHardware = new SandCrab(new Vector2(vec2.x, vec2.y), HardwareType.DOUBLESANDCRAB);
                } else if (Constants.placing_object == HardwareType.TRIPLESANDCRAB) {
                    currentPlacingHardware = new SandCrab(new Vector2(vec2.x, vec2.y), HardwareType.TRIPLESANDCRAB);
                } else if (Constants.placing_object == HardwareType.PDP) {
                    currentPlacingHardware = new PowerDistributionPanel(new Vector2(vec2.x, vec2.y), HardwareType.PDP);
                } else if (Constants.placing_object == HardwareType.VRM) {
                    currentPlacingHardware = new VoltageRegulatorModule(new Vector2(vec2.x, vec2.y), HardwareType.VRM);
                } else if (Constants.placing_object == HardwareType.ROBORIO) {
                    currentPlacingHardware = new RoboRio(new Vector2(vec2.x, vec2.y), HardwareType.ROBORIO);
                } else if (Constants.placing_object == HardwareType.TALON) {
                    currentPlacingHardware = new Talon(new Vector2(vec2.x, vec2.y), HardwareType.TALON);
                } else if (Constants.placing_object == HardwareType.PCM) {
                    currentPlacingHardware = new PneumaticsControlModule(new Vector2(vec2.x, vec2.y), HardwareType.PCM);
                } else if (Constants.placing_object == HardwareType.SPARK) {
                    currentPlacingHardware = new Spark(new Vector2(vec2.x, vec2.y), HardwareType.SPARK);
                } else if (Constants.placing_object == HardwareType.NEO) {
                    currentPlacingHardware = new NEO(new Vector2(vec2.x, vec2.y), HardwareType.NEO);
                } else if (Constants.placing_object == HardwareType.MOTOR775) {
                    currentPlacingHardware = new Motor775(new Vector2(vec2.x, vec2.y), HardwareType.MOTOR775);
                } else if (Constants.placing_object == HardwareType.BREAKER) {
                    currentPlacingHardware = new Breaker(new Vector2(vec2.x, vec2.y), HardwareType.BREAKER);
                } else if (Constants.placing_object == HardwareType.FALCON) {
                    currentPlacingHardware = new Falcon(new Vector2(vec2.x, vec2.y), HardwareType.FALCON);
                } else if (Constants.placing_object == HardwareType.BATTERY) {
                    currentPlacingHardware = new Battery(new Vector2(vec2.x, vec2.y), HardwareType.BATTERY);
                }
            }

            if (Constants.placing_object != HardwareType.WIRE) {
                handleHardware(Constants.placing_object);
            } else {
                handleCable();
            }

        }

        CableManager.update(renderer, batch, camera);
        HardwareManager.update(renderer, batch, camera);

        if(CableManager.currentCable != null) {
            CableManager.currentCable.render(renderer, camera);
            if(CableManager.currentCable.connection1 != null) {
                CableManager.currentCable.connection1.renderConnectors(batch);
            }
            if(CableManager.currentCable.connection2 != null) {
                CableManager.currentCable.connection2.renderConnectors(batch);
            }
        }


        if(Constants.placing_object != null) {
            if (Constants.placing_object == HardwareType.WIRE) {
                drawPlacing(vec2.x, vec2.y);
            } else if (currentPlacingHardware != null && currentPlacingHardware.type == Constants.placing_object) {
                currentPlacingHardware.update(batch, renderer, camera);
            }
        }

        manager.update(delta);

        batch.setProjectionMatrix(camera.getCamera().combined);
        batch.begin();
        if(!drawString.equals("")) {
            hoverFont.draw(batch, drawString, drawLoc.x + (-layout.width) / 2, drawLoc.y + 60);
        }
        batch.end();
        drawString = "";
    }

    private void handleHardware(HardwareType type) {
        CableManager.currentCable = null;

        Vector2 vec2 = Tools.mouseScreenToWorld(camera);

        if(Gdx.input.isButtonJustPressed(Input.Buttons.LEFT) && Gdx.input.getX() <= Gdx.graphics.getWidth() - 200) {
            if(Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)) {
                SnapGrid.calculateSnap(vec2);
            }

            HardwareManager.currentHardware = null;
            Constants.placing_object = null;
            HardwareManager.movingObject = false;

            HardwareManager.addHardware(vec2.x, vec2.y, type);

        }
    }

    private void drawPlacing(float x, float y) {
        renderer.begin(ShapeRenderer.ShapeType.Filled);
        renderer.setColor(Color.RED);
        renderer.circle(x, y, 5);
        renderer.end();
    }

    public static void setHoverDraw(Vector2 loc, String string) {
        drawString = string;
        drawLoc = loc;
        layout.setText(hoverFont, string);
    }


    private void handleCable() {
        HardwareManager.currentHardware = null;

        Vector3 vec = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
        camera.getCamera().unproject(vec);
        Vector2 vec2 = new Vector2(vec.x, vec.y);


        if(Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            if(Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)) {
                SnapGrid.calculateSnap(vec2);
            }

            CableManager.currentCable = null;
            CableManager.addCable(vec2.x, vec2.y);
            Constants.placing_object = null;

        }
    }

    @Override
    public void dispose() {
        renderer.dispose();
        stage.dispose();
    }

    @Override
    public void show() { }
    @Override
    public void resize(int width, int height) { }
    @Override
    public void pause() { }
    @Override
    public void resume() { }
    @Override
    public void hide() { }
}
