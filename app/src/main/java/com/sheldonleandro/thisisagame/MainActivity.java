package com.sheldonleandro.thisisagame;
import android.util.DisplayMetrics;
import org.andengine.engine.handler.IUpdateHandler;
import org.andengine.engine.handler.timer.ITimerCallback;
import org.andengine.engine.handler.timer.TimerHandler;
import org.andengine.entity.modifier.MoveModifier;
import org.andengine.entity.modifier.MoveXModifier;
import org.andengine.entity.scene.IOnSceneTouchListener;
import org.andengine.entity.scene.background.Background;
import org.andengine.entity.sprite.Sprite;
import org.andengine.input.touch.TouchEvent;
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlas;
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlasTextureRegionFactory;
import org.andengine.opengl.texture.region.TextureRegion;
import org.andengine.ui.activity.SimpleBaseGameActivity;
import org.andengine.engine.camera.Camera;
import org.andengine.engine.options.EngineOptions;
import org.andengine.engine.options.ScreenOrientation;
import org.andengine.engine.options.resolutionpolicy.RatioResolutionPolicy;
import org.andengine.entity.scene.Scene;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Random;

public class MainActivity extends SimpleBaseGameActivity implements IOnSceneTouchListener{

    private Camera camera;

    private Scene mainScene;

    private BitmapTextureAtlas ninjaActorBitmapTextureAtlas;

    private TextureRegion ninjaActorTextureRegion;

    private Sprite ninjaActor;

    private TextureRegion targetTextureRegion;

    private LinkedList targetLinkedList;

    private LinkedList targetsToBeAdded;

    private TextureRegion projectileTextureRegion;

    private LinkedList projectileLinkedList;

    private LinkedList projectilesToBeAdded;

    @Override
    public EngineOptions onCreateEngineOptions(){

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

        camera = new Camera(0, 0, displayMetrics.widthPixels,
                displayMetrics.heightPixels);

        return new EngineOptions(true, ScreenOrientation.LANDSCAPE_FIXED,
                new RatioResolutionPolicy(displayMetrics.widthPixels, displayMetrics.heightPixels),
                camera);
    }

    @Override
    protected void onCreateResources(){

        BitmapTextureAtlasTextureRegionFactory.setAssetBasePath("gfx/");

        ninjaActorBitmapTextureAtlas = new BitmapTextureAtlas(mEngine.getTextureManager(),512,
                512);

        ninjaActorTextureRegion = BitmapTextureAtlasTextureRegionFactory.createFromAsset(
                this.ninjaActorBitmapTextureAtlas,this,"player.png",0,
                0);

        targetTextureRegion = BitmapTextureAtlasTextureRegionFactory
                .createFromAsset(this.ninjaActorBitmapTextureAtlas, this, "target.png",
                        128, 0);

        projectileTextureRegion = BitmapTextureAtlasTextureRegionFactory
                .createFromAsset(this.ninjaActorBitmapTextureAtlas, this,
                        "projectile.png", 64, 0);

        mEngine.getTextureManager().loadTexture(ninjaActorBitmapTextureAtlas);
    }

    @Override
    protected Scene onCreateScene(){

        // 1 - Create new scene
        mainScene = new Scene();
        Background background = new Background(0.09804f, 0.6274f, 0.8784f);
        mainScene.setBackground(background);
        mainScene.setBackgroundEnabled(true);

        final float playerX = this.ninjaActorTextureRegion.getWidth() / 2;

        final float playerY = ((camera.getHeight() - ninjaActorTextureRegion.getHeight()) / 2);

        ninjaActor = new Sprite(playerX, playerY, this.ninjaActorTextureRegion,
                getVertexBufferObjectManager());

        mainScene.attachChild(ninjaActor);

        targetLinkedList = new LinkedList();
        targetsToBeAdded = new LinkedList();

        projectileLinkedList = new LinkedList();
        projectilesToBeAdded = new LinkedList();

        createSpriteSpawnTimeHandler();

        IUpdateHandler detect = new IUpdateHandler() {

            @Override
            public void reset() {
            }

            @Override
            public void onUpdate(float pSecondsElapsed) {

                Iterator<Sprite> targets = targetLinkedList.iterator();

                Sprite _target = null;

                while (targets.hasNext()) {

                    _target = targets.next();

                    if (_target.getX() <= _target.getWidth()) {
                        removeSprite(_target, targets);
                        break;
                    }

                    Iterator<Sprite> projectiles = projectileLinkedList.iterator();

                    Sprite _projectile = null;

                    while (projectiles.hasNext()) {
                        _projectile = projectiles.next();
                        if (_projectile.getX() >= camera.getWidth()
                                || _projectile.getY() >= camera.getHeight()
                                + _projectile.getHeight()
                                || _projectile.getY() <= _projectile.getHeight()) {
                            removeSprite(_projectile, projectiles);
                            continue;
                        }
                    }

                    if (_target.collidesWith(_projectile)) {
                        removeSprite(_projectile, projectiles);
                        removeSprite(_target, targets);
                        break;
                    }
                }

                projectileLinkedList.addAll(projectilesToBeAdded);
                projectilesToBeAdded.clear();

                targetLinkedList.addAll(targetsToBeAdded);
                targetsToBeAdded.clear();
            }

        };

        mainScene.registerUpdateHandler(detect);
        mainScene.setOnSceneTouchListener(this);
        return mainScene;
    }

    public void addTarget() {
        Random rand = new Random();
        float x = camera.getWidth() + targetTextureRegion.getWidth();
        float minY = targetTextureRegion.getHeight();
        float maxY = (int) (camera.getHeight() - targetTextureRegion.getHeight());
        float rangeY = maxY - minY;
        float y = rand.nextInt((int) rangeY) + minY;

        Sprite target = new Sprite(x-15, y, targetTextureRegion.deepCopy(), getVertexBufferObjectManager());
        target.setScale(3);

        mainScene.attachChild(target);
        int minDuration = 2;
        int maxDuration = 4;
        int rangeDuration = maxDuration - minDuration;
        int actualDuration = rand.nextInt(rangeDuration) + minDuration;
        MoveXModifier mod = new MoveXModifier(actualDuration, target.getX(),
                -target.getWidth());
        target.registerEntityModifier(mod.deepCopy());
        targetsToBeAdded.add(target);
    }

    private void createSpriteSpawnTimeHandler() {

        float mEffectSpawnDelay = 1f;

        TimerHandler spriteTimerHandler = new TimerHandler(mEffectSpawnDelay, true,
                new ITimerCallback() {

            @Override
            public void onTimePassed(TimerHandler pTimerHandler) {
                addTarget();
            }
        });

        getEngine().registerUpdateHandler(spriteTimerHandler);
    }

    public void removeSprite(final Sprite _sprite, Iterator it) {

        runOnUpdateThread(new Runnable() {
            @Override
            public void run() {
                mainScene.detachChild(_sprite);
            }
        });

        it.remove();
    }

    private void shootProjectile(final float pX, final float pY) {

        int offX = (int) (pX - ninjaActor.getX());

        int offY = (int) (pY - ninjaActor.getY());

        if (offX <= 0)
            return;

        final Sprite projectile;

        projectile = new Sprite(ninjaActor.getX(), ninjaActor.getY(),
                projectileTextureRegion.deepCopy(),
                getVertexBufferObjectManager());

        mainScene.attachChild(projectile);

        int realX = (int) (camera.getWidth() + projectile.getWidth() / 2.0f);

        float ratio = (float) offY / (float) offX;

        int realY = (int) ((realX * ratio) + projectile.getY());

        int offRealX = (int) (realX - projectile.getX());

        int offRealY = (int) (realY - projectile.getY());

        float length = (float) Math.sqrt((offRealX * offRealX)
                + (offRealY * offRealY));

        float velocity = 480.0f / 1.0f; // 480 pixels / 1 sec

        float realMoveDuration = length / velocity;

        MoveModifier mod = new MoveModifier(realMoveDuration,
                projectile.getX(), realX, projectile.getY(), realY);
        projectile.registerEntityModifier(mod.deepCopy());

        projectilesToBeAdded.add(projectile);
    }

    @Override
    public boolean onSceneTouchEvent(Scene pScene, TouchEvent pSceneTouchEvent) {
        if (pSceneTouchEvent.getAction() == TouchEvent.ACTION_DOWN) {
            final float touchX = pSceneTouchEvent.getX();
            final float touchY = pSceneTouchEvent.getY();
            shootProjectile(touchX, touchY);
            return true;
        }
        return false;
    }

}