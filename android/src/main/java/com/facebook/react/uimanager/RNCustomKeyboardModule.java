package com.facebook.react.uimanager;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.RelativeLayout;

import com.facebook.react.ReactApplication;
import com.facebook.react.ReactRootView;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.UiThreadUtil;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.facebook.react.views.textinput.ReactEditText;

public class RNCustomKeyboardModule extends ReactContextBaseJavaModule {
    private final int TAG_ID = 0xdeadbeaf;
    private final ReactApplicationContext reactContext;

    public RNCustomKeyboardModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
    }

    Handler handle = new Handler(Looper.getMainLooper());

    private ReactEditText getEditById(int id) {
        try {
            UIViewOperationQueue uii = this.getReactApplicationContext().getNativeModule(UIManagerModule.class).getUIImplementation().getUIViewOperationQueue();
            return (ReactEditText) uii.getNativeViewHierarchyManager().resolveView(id);
        } catch (Exception e) {
            e.printStackTrace();
        }return null;
    }

    @ReactMethod
    public void install(final int tag, final String type) {
        try {
            UiThreadUtil.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    final Activity activity = getCurrentActivity();
                    final ReactEditText edit = getEditById(tag);
                    if (edit == null) {
                        return;
                    }

                    edit.setTag(TAG_ID, createCustomKeyboard(activity, tag, type));

                    edit.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                        @Override
                        public void onFocusChange(final View v, boolean hasFocus) {
                            if (hasFocus) {
                                UiThreadUtil.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            ((InputMethodManager) getReactApplicationContext().getSystemService(Activity.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(v.getWindowToken(), 0);
                                            View keyboard = (View)edit.getTag(TAG_ID);
                                            if(keyboard != null){
                                                if (keyboard.getParent() == null) {
                                                    activity.addContentView(keyboard, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                                                }else{
                                                    keyboard.setVisibility(View.VISIBLE);
                                                }
                                            }
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                    }
                                });
                            } else {
                                try {
                                    View keyboard = (View)edit.getTag(TAG_ID);
                                    //                            if (keyboard.getParent() != null) {
                                    //                                ((ViewGroup) keyboard.getParent()).removeView(keyboard);
                                    //                            }
                                    if(keyboard != null){
                                        keyboard.setVisibility(View.GONE);
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                            try {
                                getReactApplicationContext()
                                        .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                                        .emit("CustomKeyboard_Resp", hasFocus);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });

                    edit.setOnTouchListener(new View.OnTouchListener() {
                        @Override
                        public boolean onTouch(View v, MotionEvent event) {
                            if(event.getAction() == MotionEvent.ACTION_UP){
    //                            callback.invoke(false);
                                edit.setFocusable(true);
                                edit.setFocusableInTouchMode(true);
                                edit.requestFocusFromJS();
                                UiThreadUtil.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        ((InputMethodManager) getReactApplicationContext().getSystemService(Activity.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(edit.getWindowToken(), 0);
                                        View keyboard = (View)edit.getTag(TAG_ID);
                                        if(keyboard != null){
                                            if (keyboard.getParent() == null) {
                                                activity.addContentView(keyboard, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                                            }else {
                                                keyboard.setVisibility(View.VISIBLE);
                                            }
                                        }
                                    }
                                });
                            }
                            return false;
                        }
                    });

    //                edit.setOnClickListener(new View.OnClickListener(){
    //                    @Override
    //                    public void onClick(final View v) {
    //
    //
    //                    }
    //                });

                    edit.setOnLongClickListener(new View.OnLongClickListener() {
                        @Override
                        public boolean onLongClick(final View v) {
                            return true;
                        }
                    });
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    ReactRootView rootView = null;

    private View createCustomKeyboard(Activity activity, int tag, String type) {
        RelativeLayout layout = new RelativeLayout(activity);
        rootView = new ReactRootView(this.getReactApplicationContext());
        rootView.setBackgroundColor(Color.WHITE);

        Bundle bundle = new Bundle();
        bundle.putInt("tag", tag);
        bundle.putString("type", type);
        rootView.startReactApplication(
                ((ReactApplication) activity.getApplication()).getReactNativeHost().getReactInstanceManager(),
                "CustomKeyboard",
                bundle);

        final float scale = activity.getResources().getDisplayMetrics().density;
        RelativeLayout.LayoutParams lParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Math.round(250*scale));
        lParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
        lParams.bottomMargin = 0;
        layout.addView(rootView, lParams);
//        activity.addContentView(layout, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        return layout;
    }
    

    @ReactMethod
    public void uninstall(final int tag) {
        UiThreadUtil.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final Activity activity = getCurrentActivity();
                final ReactEditText edit = getEditById(tag);
                if (edit == null) {
                    return;
                }

                edit.setTag(TAG_ID, null);
            }
        });
    }

    @ReactMethod
    public void insertText(final int tag, final String text) {
        UiThreadUtil.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final Activity activity = getCurrentActivity();
                final ReactEditText edit = getEditById(tag);
                if (edit == null) {
                    return;
                }

                int start = Math.max(edit.getSelectionStart(), 0);
                int end = Math.max(edit.getSelectionEnd(), 0);
                edit.getText().replace(Math.min(start, end), Math.max(start, end),
                        text, 0, text.length());
            }
        });
    }

    @ReactMethod
    public void backSpace(final int tag) {
        UiThreadUtil.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final Activity activity = getCurrentActivity();
                final ReactEditText edit = getEditById(tag);
                if (edit == null) {
                    return;
                }

                int start = Math.max(edit.getSelectionStart(), 0);
                int end = Math.max(edit.getSelectionEnd(), 0);
                if (start != end) {
                    edit.getText().delete(start, end);
                } else if (start > 0){
                    edit.getText().delete(start - 1, end);
                }
            }
        });
    }

    @ReactMethod
    public void doDelete(final int tag) {
        UiThreadUtil.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final Activity activity = getCurrentActivity();
                final ReactEditText edit = getEditById(tag);
                if (edit == null) {
                    return;
                }

                int start = Math.max(edit.getSelectionStart(), 0);
                int end = Math.max(edit.getSelectionEnd(), 0);
                if (start != end) {
                    edit.getText().delete(0, end);
                } else if (start > 0){
                    edit.getText().delete(0, end);
                }
            }
        });
    }

    @ReactMethod
    public void moveLeft(final int tag) {
        UiThreadUtil.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final Activity activity = getCurrentActivity();
                final ReactEditText edit = getEditById(tag);
                if (edit == null) {
                    return;
                }

                int start = Math.max(edit.getSelectionStart(), 0);
                int end = Math.max(edit.getSelectionEnd(), 0);
                if (start != end) {
                    edit.setSelection(start, start);
                } else {
                    edit.setSelection(start - 1, start - 1);
                }
            }
        });
    }

    @ReactMethod
    public void moveRight(final int tag) {
        UiThreadUtil.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final Activity activity = getCurrentActivity();
                final ReactEditText edit = getEditById(tag);
                if (edit == null) {
                    return;
                }

                int start = Math.max(edit.getSelectionStart(), 0);
                int end = Math.max(edit.getSelectionEnd(), 0);
                if (start != end) {
                    edit.setSelection(end, end);
                } else if (start > 0){
                    edit.setSelection(end + 1, end + 1);
                }
            }
        });
    }

    @ReactMethod
    public void switchSystemKeyboard(final int tag) {
        UiThreadUtil.runOnUiThread(new Runnable() {
            @Override
            public void run() {

                try {
                    final Activity activity = getCurrentActivity();
                    final ReactEditText edit = getEditById(tag);
                    if (edit == null) {
                        return;
                    }
                    edit.clearFocus();
                    View keyboard = (View)edit.getTag(TAG_ID);
                    if(keyboard != null){
                        if (keyboard.getParent() != null) {
    //                    ((ViewGroup) keyboard.getParent()).removeView(keyboard);
                            keyboard.setVisibility(View.GONE);
                        }
                    }

//                UiThreadUtil.runOnUiThread(new Runnable() {
//
//                    @Override
//                    public void run() {
//                        ((InputMethodManager) getReactApplicationContext().getSystemService(Activity.INPUT_METHOD_SERVICE)).showSoftInput(edit, InputMethodManager.SHOW_IMPLICIT);
//                    }
//                });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public String getName() {
      return "CustomKeyboard";
    }
}
