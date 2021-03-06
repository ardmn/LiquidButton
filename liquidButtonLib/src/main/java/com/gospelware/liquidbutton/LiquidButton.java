package com.gospelware.liquidbutton;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.gospelware.liquidbutton.controller.BaseController;
import com.gospelware.liquidbutton.controller.PourFinishController;
import com.gospelware.liquidbutton.controller.PourStartController;
import com.gospelware.liquidbutton.controller.TickController;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ricogao on 12/05/2016.
 */
public class LiquidButton extends View {

    private static final String TAG = LiquidButton.class.getSimpleName();

    private BaseController mController;
    private List<BaseController> mControllers;
    private PourFinishListener listener;
    private boolean isFillAfter;
    private boolean fillAfterFlag;
    private Animator mAnimator;

    public LiquidButton(Context context) {
        this(context, null);
    }

    public LiquidButton(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LiquidButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public void setController(BaseController controller) {
        this.mController = controller;
        if (hasController()) {
            mController.setCheckView(this);
        }
    }

    public void setFillAfter(boolean fillAfter) {
        this.isFillAfter = fillAfter;
    }

    private boolean hasController() {
        return mController != null;
    }

    public BaseController getController() {
        return mController;
    }

    public void setControllers(List<BaseController> controllers) {
        this.mControllers = controllers;
        if (hasControllers()) {
            for (BaseController controller : mControllers) {
                controller.setCheckView(this);
            }
        }
    }

    private boolean hasControllers() {
        return mControllers != null && mControllers.size() > 0;
    }

    public List<BaseController> getControllers() {
        return mControllers;
    }

    /**
     * Basic Animations to build the LiquidButton
     */
    protected void init() {
        List<BaseController> controllers = new ArrayList<>();
        PourStartController startController = new PourStartController();
        PourFinishController finishController = new PourFinishController();
        TickController tickController = new TickController();
        controllers.add(startController);
        controllers.add(finishController);
        controllers.add(tickController);
        setControllers(controllers);
    }

    public interface PourFinishListener {
        void onPourFinish();
    }

    public void setPourFinishListener(PourFinishListener listener) {
        this.listener = listener;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        int width = getWidth();
        int height = getHeight();

        if (hasController()) {
            getController().getMeasure(width, height);
        } else if (hasControllers()) {
            for (BaseController controller : getControllers()) {
                controller.getMeasure(width, height);
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        BaseController controller=getRunningController();
        if(controller!=null){
            controller.draw(canvas);
        }

        //if the fill after flag is on, draw the lastFrame
        if (fillAfterFlag) {
            onFillAfter(canvas);
        }
    }

    public BaseController getRunningController(){
        if(hasController()&&getController().isRunning()){
            return getController();
        }else if(hasControllers()){
            for(BaseController controller:getControllers()){
                if(controller.isRunning()){
                    return controller;
                }
            }
        }

        return null;

    }

    public void onFillAfter(Canvas canvas) {
        if (hasController() && !getController().isRunning()) {
            getController().draw(canvas);
        } else if (hasControllers()) {
            //draw the last frame of the last controller
            BaseController controller = getControllers().get(getControllers().size() - 1);
            if (!controller.isRunning()) {
                controller.draw(canvas);
            }
        }
    }

    public Animator buildAnimator() {
        if (hasController()) {
            return getController().getAnimator();
        } else if (hasControllers()) {
            List<Animator> animators = new ArrayList<>();
            for (BaseController controller : getControllers()) {
                animators.add(controller.getAnimator());
            }
            AnimatorSet animatorSet = new AnimatorSet();
            animatorSet.playSequentially(animators);
            return animatorSet;
        }

        return null;
    }

    public void startPour() {

        //clear the fillAfterFlag
        if (fillAfterFlag) {
            fillAfterFlag = false;
        }

        if (mAnimator == null) {
            mAnimator = buildAnimator();
            mAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    onPourEnd();
                    finishPour();
                }
            });
        }

        if (mAnimator != null&&!mAnimator.isRunning()) {
            mAnimator.start();
        } else {
            Log.e(TAG, "No controller or Animator is been build");
        }
    }

    public void onPourEnd() {
        //turn the fillAfter flag ON if it's been set
        fillAfterFlag = isFillAfter;

        if (fillAfterFlag) {
            postInvalidate();
        }
    }


    public void finishPour() {
        if (listener != null) {
            listener.onPourFinish();
        }
    }

}
