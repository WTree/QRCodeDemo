package view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.*;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import com.lyhh.library_qrscan.R;
import utils.BGAQRCodeUtil;
import utils.SystemUtils;


public class ScanBoxView extends View {
    private int mMoveStepDistance;
    private int mAnimDelayTime;

    private Rect mFramingRect;
    private float mScanLineTop;
    private float mScanLineLeft;
    private Paint mPaint;

    private int mMaskColor;
    private int mCornerColor;
    private int mCornerLength;
    private int mCornerSize;
    private int mRectWidth;
    private int mRectHeight;
    private int mScanLineSize;
    private int mScanLineColor;
    private int mScanLineMargin;
    private Drawable mCustomScanLineDrawable;
    private Bitmap mScanLineBitmap;
    private int mBorderSize;
    private int mBorderColor;
    private int mAnimTime;
    private boolean mIsCenterVertical;
    private Drawable mCustomGridScanLineDrawable;
    private Bitmap mGridScanLineBitmap;
    private float mGridScanLineBottom;
    private float mGridScanLineRight;
    private int mTopOffset;
    private int mToolbarHeight;

    private Bitmap mOriginQRCodeScanLineBitmap;
    private Bitmap mOriginQRCodeGridScanLineBitmap;

    private Bitmap mCornerBitmap;
    private Bitmap mCornerBitmap2;
    private Bitmap mCornerBitmap3;
    private Bitmap mCornerBitmap4;



    private float mHalfCornerSize;

    private int textWith = 0;
    private int textMargin = 0;

    public ScanBoxView(Context context) {
        this(context, null);
    }

    public ScanBoxView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ScanBoxView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mMaskColor = Color.parseColor("#33FFFFFF");
        mCornerLength = SystemUtils.dp2px(context, 20);
        mCornerSize = SystemUtils.dp2px(context, 3);
        mScanLineSize = SystemUtils.dp2px(context, 1);
        mRectWidth = SystemUtils.dp2px(context, 200);
        textMargin = SystemUtils.dp2px(context,30);
        mCustomScanLineDrawable = null;
        mScanLineBitmap = null;
        mBorderSize = SystemUtils.dp2px(context, 1);
        mMoveStepDistance = SystemUtils.dp2px(context, 2);

        textWith = getTextViewLength(getContext().getString(R.string.msg_scan_hint));

        initCustomAttrs(context, attrs);
    }

    //获取文字长度
    public int getTextViewLength(String text) {
        Rect rect = new Rect();
        Paint paint = new Paint();
        paint.setTextSize(sp2px(getContext(), 13));
        paint.setColor(Color.WHITE);
        paint.getTextBounds(text, 0, text.length(), rect);
        return rect.width();
    }

    public void initCustomAttrs(Context context, AttributeSet attrs) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.QRCodeView);
        final int count = typedArray.getIndexCount();
        for (int i = 0; i < count; i++) {
            initCustomAttr(typedArray.getIndex(i), typedArray);
        }
        typedArray.recycle();

        afterInitCustomAttrs();
    }

    private void initCustomAttr(int attr, TypedArray typedArray) {
        if (attr == R.styleable.QRCodeView_qrcv_topOffset) {
            mTopOffset = typedArray.getDimensionPixelSize(attr, 0);
        } else if (attr == R.styleable.QRCodeView_qrcv_cornerSize) {
            mCornerSize = typedArray.getDimensionPixelSize(attr, mCornerSize);
        } else if (attr == R.styleable.QRCodeView_qrcv_cornerLength) {
            mCornerLength = typedArray.getDimensionPixelSize(attr, mCornerLength);
        } else if (attr == R.styleable.QRCodeView_qrcv_scanLineSize) {
            mScanLineSize = typedArray.getDimensionPixelSize(attr, mScanLineSize);
        } else if (attr == R.styleable.QRCodeView_qrcv_rectWidth) {
            mRectWidth = typedArray.getDimensionPixelSize(attr, mRectWidth);
        } else if (attr == R.styleable.QRCodeView_qrcv_maskColor) {
            mMaskColor = typedArray.getColor(attr, mMaskColor);
        } else if (attr == R.styleable.QRCodeView_qrcv_cornerColor) {
            mCornerColor = typedArray.getColor(attr, Color.WHITE);
        } else if (attr == R.styleable.QRCodeView_qrcv_scanLineColor) {
            mScanLineColor = typedArray.getColor(attr, Color.WHITE);
        } else if (attr == R.styleable.QRCodeView_qrcv_scanLineMargin) {
            mScanLineMargin = typedArray.getDimensionPixelSize(attr, 0);
        } else if (attr == R.styleable.QRCodeView_qrcv_customScanLineDrawable) {
            mCustomScanLineDrawable = typedArray.getDrawable(attr);
        } else if (attr == R.styleable.QRCodeView_qrcv_borderSize) {
            mBorderSize = typedArray.getDimensionPixelSize(attr, mBorderSize);
        } else if (attr == R.styleable.QRCodeView_qrcv_borderColor) {
            mBorderColor = typedArray.getColor(attr, Color.WHITE);
        } else if (attr == R.styleable.QRCodeView_qrcv_animTime) {
            mAnimTime = typedArray.getInteger(attr, 1000);
        } else if (attr == R.styleable.QRCodeView_qrcv_isCenterVertical) {
            mIsCenterVertical = typedArray.getBoolean(attr, false);
        } else if (attr == R.styleable.QRCodeView_qrcv_customGridScanLineDrawable) {
            mCustomGridScanLineDrawable = typedArray.getDrawable(attr);
        } else if (attr == R.styleable.QRCodeView_qrcv_toolbarHeight) {
            mToolbarHeight = typedArray.getDimensionPixelSize(attr, 0);
        }
    }

    private void afterInitCustomAttrs() {
        if (mCustomGridScanLineDrawable != null) {
            mOriginQRCodeGridScanLineBitmap = ((BitmapDrawable) mCustomGridScanLineDrawable).getBitmap();
        }
        if (mOriginQRCodeGridScanLineBitmap == null) {
            mOriginQRCodeGridScanLineBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.scan_grid);
            mOriginQRCodeGridScanLineBitmap = BGAQRCodeUtil.makeTintBitmap(mOriginQRCodeGridScanLineBitmap, mScanLineColor);
        }

        if (mCustomScanLineDrawable != null) {
            mOriginQRCodeScanLineBitmap = ((BitmapDrawable) mCustomScanLineDrawable).getBitmap();
        }
        if (mOriginQRCodeScanLineBitmap == null) {
            mOriginQRCodeScanLineBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.line);
            mOriginQRCodeScanLineBitmap = BGAQRCodeUtil.makeTintBitmap(mOriginQRCodeScanLineBitmap, mScanLineColor);
        }

        if (mCornerBitmap == null) {
            mCornerBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.bg_corner);
            mCornerBitmap2 = rotateBitmap(mCornerBitmap,90);
            mCornerBitmap3 = rotateBitmap(mCornerBitmap,270);
            mCornerBitmap4 = rotateBitmap(mCornerBitmap,180);
        }

        mTopOffset += mToolbarHeight;
        mHalfCornerSize = 1.0f * mCornerSize / 2;


        setIsBarcode();
    }

    @Override
    public void onDraw(Canvas canvas) {
        if (mFramingRect == null) {
            return;
        }

        // 画遮罩层
        drawMask(canvas);

        // 画边框线
        drawBorderLine(canvas);

        // 画四个直角的线
        drawCornerLine(canvas);

        // 画扫描线
        drawScanLine(canvas);

        // 移动扫描线的位置
        moveScanLine();

        // 画扫描提示
        drawText(canvas);

    }

    /**
     * 画遮罩层
     *
     * @param canvas
     */
    private void drawMask(Canvas canvas) {
        int width = canvas.getWidth();
        int height = canvas.getHeight();

        if (mMaskColor != Color.TRANSPARENT) {
            mPaint.setStyle(Paint.Style.FILL);
            mPaint.setColor(mMaskColor);
            canvas.drawRect(0, 0, width, mFramingRect.top, mPaint);
            canvas.drawRect(0, mFramingRect.top, mFramingRect.left, mFramingRect.bottom + 1, mPaint);
            canvas.drawRect(mFramingRect.right + 1, mFramingRect.top, width, mFramingRect.bottom + 1, mPaint);
            canvas.drawRect(0, mFramingRect.bottom + 1, width, height, mPaint);
        }
    }

    /**
     * 画边框线
     *
     * @param canvas
     */
    private void drawBorderLine(Canvas canvas) {
        if (mBorderSize > 0) {
            mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setColor(mBorderColor);
            mPaint.setStrokeWidth(mBorderSize);
            canvas.drawRect(mFramingRect, mPaint);
        }
    }

    /**
     * 画四个直角的线
     *
     * @param canvas
     */
    private void drawCornerLine(Canvas canvas) {
        if (mHalfCornerSize > 0) {
            mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setColor(mCornerColor);
            mPaint.setStrokeWidth(mCornerSize);
//            canvas.drawLine(mFramingRect.left - mHalfCornerSize, mFramingRect.top, mFramingRect.left - mHalfCornerSize + mCornerLength, mFramingRect.top, mPaint);
//            canvas.drawLine(mFramingRect.left, mFramingRect.top - mHalfCornerSize, mFramingRect.left, mFramingRect.top - mHalfCornerSize + mCornerLength, mPaint);
//            canvas.drawLine(mFramingRect.right + mHalfCornerSize, mFramingRect.top, mFramingRect.right + mHalfCornerSize - mCornerLength, mFramingRect.top, mPaint);
//            canvas.drawLine(mFramingRect.right, mFramingRect.top - mHalfCornerSize, mFramingRect.right, mFramingRect.top - mHalfCornerSize + mCornerLength, mPaint);
//
//            canvas.drawLine(mFramingRect.left - mHalfCornerSize, mFramingRect.bottom, mFramingRect.left - mHalfCornerSize + mCornerLength, mFramingRect.bottom, mPaint);
//            canvas.drawLine(mFramingRect.left, mFramingRect.bottom + mHalfCornerSize, mFramingRect.left, mFramingRect.bottom + mHalfCornerSize - mCornerLength, mPaint);
//            canvas.drawLine(mFramingRect.right + mHalfCornerSize, mFramingRect.bottom, mFramingRect.right + mHalfCornerSize - mCornerLength, mFramingRect.bottom, mPaint);
//            canvas.drawLine(mFramingRect.right, mFramingRect.bottom + mHalfCornerSize, mFramingRect.right, mFramingRect.bottom + mHalfCornerSize - mCornerLength, mPaint);

            if (mCornerBitmap != null) {
                canvas.drawBitmap(mCornerBitmap, mFramingRect.left - mHalfCornerSize, mFramingRect.top, mPaint);
                canvas.drawBitmap(mCornerBitmap2, mFramingRect.right + mHalfCornerSize - mCornerBitmap.getWidth(), mFramingRect.top, mPaint);
                canvas.drawBitmap(mCornerBitmap3, mFramingRect.left - mHalfCornerSize, mFramingRect.bottom - mCornerBitmap.getWidth(), mPaint);
                canvas.drawBitmap(mCornerBitmap4, mFramingRect.right + mHalfCornerSize - mCornerBitmap.getWidth(), mFramingRect.bottom - mCornerBitmap.getWidth(), mPaint);
            }
        }
    }

    /**
     * 画扫描线
     *
     * @param canvas
     */
    private void drawScanLine(Canvas canvas) {
        if (mGridScanLineBitmap != null) {
            RectF dstGridRectF = new RectF(mFramingRect.left + mHalfCornerSize + mScanLineMargin, mFramingRect.top + mHalfCornerSize + 0.5f, mFramingRect.right - mHalfCornerSize - mScanLineMargin, mGridScanLineBottom);

            Rect srcRect = new Rect(0, (int) (mGridScanLineBitmap.getHeight() - dstGridRectF.height()), mGridScanLineBitmap.getWidth(), mGridScanLineBitmap.getHeight());

            if (srcRect.top < 0) {
                srcRect.top = 0;
                dstGridRectF.top = dstGridRectF.bottom - srcRect.height();
            }

            canvas.drawBitmap(mGridScanLineBitmap, srcRect, dstGridRectF, mPaint);
        } else if (mScanLineBitmap != null) {
            RectF lineRect = new RectF(mFramingRect.left + mHalfCornerSize + mScanLineMargin, mScanLineTop, mFramingRect.right - mHalfCornerSize - mScanLineMargin, mScanLineTop + mScanLineBitmap.getHeight());
            canvas.drawBitmap(mScanLineBitmap, null, lineRect, mPaint);
        } else {
            mPaint.setStyle(Paint.Style.FILL);
            mPaint.setColor(mScanLineColor);
            canvas.drawRect(mFramingRect.left + mHalfCornerSize + mScanLineMargin, mScanLineTop, mFramingRect.right - mHalfCornerSize - mScanLineMargin, mScanLineTop + mScanLineSize, mPaint);
        }
    }

    /**
     * 绘制扫描文字
     *
     * @param canvas
     */
    private void drawText(Canvas canvas) {
        mPaint.setTextSize(sp2px(getContext(), 13));
        mPaint.setColor(Color.WHITE);
        int textSpace = (mFramingRect.right - mFramingRect.left - textWith) / 2;
        canvas.drawText(getContext().getString(R.string.msg_scan_hint), mFramingRect.left + textSpace, mFramingRect.top - textMargin, mPaint);
    }

    /**
     * 移动扫描线的位置
     */
    private void moveScanLine() {
        if (mGridScanLineBitmap == null) {
            // 处理非网格扫描图片的情况
            mScanLineTop += mMoveStepDistance;
            int scanLineSize = mScanLineSize;
            if (mScanLineBitmap != null) {
                scanLineSize = mScanLineBitmap.getHeight();
            }
            if (mScanLineTop + scanLineSize > mFramingRect.bottom - mHalfCornerSize) {
                mScanLineTop = mFramingRect.top + mHalfCornerSize + 0.5f;
            }

        } else {
            // 处理网格扫描图片的情况
            mGridScanLineBottom += mMoveStepDistance;
            if (mGridScanLineBottom > mFramingRect.bottom - mHalfCornerSize) {
                mGridScanLineBottom = mFramingRect.top + mHalfCornerSize + 0.5f;
            }
        }
        postInvalidateDelayed(mAnimDelayTime, mFramingRect.left, mFramingRect.top, mFramingRect.right, mFramingRect.bottom);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        calFramingRect();
    }

    private void calFramingRect() {
        int leftOffset = (getWidth() - mRectWidth) / 2;
        mFramingRect = new Rect(leftOffset, mTopOffset, leftOffset + mRectWidth, mTopOffset + mRectHeight);

        mGridScanLineBottom = mScanLineTop = mFramingRect.top + mHalfCornerSize + 0.5f;

    }

    public Rect getScanBoxAreaRect(int previewWidth, int previewHeight) {
        Rect rect = new Rect(mFramingRect);
        float widthRatio = 1.0f * previewWidth / getMeasuredWidth();
        float heightRatio = 1.0f * previewHeight / getMeasuredHeight();
        rect.left = (int) (rect.left * widthRatio);
        rect.right = (int) (rect.right * widthRatio);
        rect.top = (int) (rect.top * heightRatio);
        rect.bottom = (int) (rect.bottom * heightRatio);
        return rect;
    }

    public void setIsBarcode() {
        if (mCustomGridScanLineDrawable != null) {
            mGridScanLineBitmap = mOriginQRCodeGridScanLineBitmap;
        } else if (mCustomScanLineDrawable != null) {
            mScanLineBitmap = mOriginQRCodeScanLineBitmap;
        }

        mRectHeight = mRectWidth;
        mAnimDelayTime = (int) ((1.0f * mAnimTime * mMoveStepDistance) / mRectHeight);


        if (mIsCenterVertical) {
            int screenHeight = BGAQRCodeUtil.getScreenResolution(getContext()).y;
            if (mToolbarHeight == 0) {
                mTopOffset = (screenHeight - mRectHeight) / 2;
            } else {
                mTopOffset = (screenHeight - mRectHeight) / 2 - mToolbarHeight / 2;
            }
        }

        calFramingRect();

        postInvalidate();
    }

    public int getMaskColor() {
        return mMaskColor;
    }

    public void setMaskColor(int maskColor) {
        mMaskColor = maskColor;
    }

    public int getCornerColor() {
        return mCornerColor;
    }

    public void setCornerColor(int cornerColor) {
        mCornerColor = cornerColor;
    }

    public int getCornerLength() {
        return mCornerLength;
    }

    public void setCornerLength(int cornerLength) {
        mCornerLength = cornerLength;
    }

    public int getCornerSize() {
        return mCornerSize;
    }

    public void setCornerSize(int cornerSize) {
        mCornerSize = cornerSize;
    }

    public int getRectWidth() {
        return mRectWidth;
    }

    public void setRectWidth(int rectWidth) {
        mRectWidth = rectWidth;
    }

    public int getRectHeight() {
        return mRectHeight;
    }

    public void setRectHeight(int rectHeight) {
        mRectHeight = rectHeight;
    }

    public int getScanLineSize() {
        return mScanLineSize;
    }

    public void setScanLineSize(int scanLineSize) {
        mScanLineSize = scanLineSize;
    }

    public int getScanLineColor() {
        return mScanLineColor;
    }

    public void setScanLineColor(int scanLineColor) {
        mScanLineColor = scanLineColor;
    }

    public int getScanLineMargin() {
        return mScanLineMargin;
    }

    public void setScanLineMargin(int scanLineMargin) {
        mScanLineMargin = scanLineMargin;
    }


    public Drawable getCustomScanLineDrawable() {
        return mCustomScanLineDrawable;
    }

    public void setCustomScanLineDrawable(Drawable customScanLineDrawable) {
        mCustomScanLineDrawable = customScanLineDrawable;
    }

    public Bitmap getScanLineBitmap() {
        return mScanLineBitmap;
    }

    public void setScanLineBitmap(Bitmap scanLineBitmap) {
        mScanLineBitmap = scanLineBitmap;
    }

    public int getBorderSize() {
        return mBorderSize;
    }

    public void setBorderSize(int borderSize) {
        mBorderSize = borderSize;
    }

    public int getBorderColor() {
        return mBorderColor;
    }

    public void setBorderColor(int borderColor) {
        mBorderColor = borderColor;
    }

    public int getAnimTime() {
        return mAnimTime;
    }

    public void setAnimTime(int animTime) {
        mAnimTime = animTime;
    }

    public boolean isCenterVertical() {
        return mIsCenterVertical;
    }

    public void setCenterVertical(boolean centerVertical) {
        mIsCenterVertical = centerVertical;
    }

    public float getHalfCornerSize() {
        return mHalfCornerSize;
    }

    public void setHalfCornerSize(float halfCornerSize) {
        mHalfCornerSize = halfCornerSize;
    }

    public int sp2px(Context context, float sp) {
        final float fontScale = context.getResources().getDisplayMetrics().scaledDensity;
        return (int) (sp * fontScale + 0.5f);
    }

    public Bitmap rotateBitmap(Bitmap bitmap,float degrees) {
        if (bitmap != null) {
            Matrix m = new Matrix();
            try {
                m.setRotate(degrees, bitmap.getWidth() / 2, bitmap.getHeight() / 2);//90就是我们需要选择的90度
                Bitmap bmp2 = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m, true);
                return bmp2;
            } catch (Exception ex) {
                System.out.print("创建图片失败！" + ex);
            }
        }
        return bitmap;

    }
}