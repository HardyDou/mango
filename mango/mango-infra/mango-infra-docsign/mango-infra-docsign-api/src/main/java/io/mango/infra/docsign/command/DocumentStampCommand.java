package io.mango.infra.docsign.command;

import io.mango.common.contract.LocalCapabilityContract;
import io.mango.common.result.Require;
import io.mango.infra.docsign.enums.StampSide;
import io.mango.infra.docsign.enums.StampType;

import java.util.Arrays;

/**
 * Visible stamp placement using millimetres and a top-left origin.
 * <p>
 * PDF uses {@link #image()} as the visible stamp. OFD native electronic sealing uses caller-supplied
 * {@link #ofdSeal()} containing a compliant SES v4 electronic seal.
 */
@LocalCapabilityContract
public final class DocumentStampCommand {

    private final StampType type;

    private final int page;

    private final double x;

    private final double y;

    private final double width;

    private final double height;

    private final StampSide side;

    private final double margin;

    private final int clipNumber;

    private final byte[] image;

    private final byte[] ofdSeal;

    private DocumentStampCommand(Builder builder) {
        Require.notNull(builder.type, "印章类型不能为空");
        Require.isTrue(builder.width > 0 && builder.height > 0, "印章宽高必须大于 0");
        if (builder.type == StampType.NORMAL) {
            Require.isTrue(builder.page > 0, "普通章页码必须从 1 开始");
            Require.isTrue(builder.x >= 0 && builder.y >= 0, "普通章坐标不能为负数");
        }
        this.type = builder.type;
        this.page = builder.page;
        this.x = builder.x;
        this.y = builder.y;
        this.width = builder.width;
        this.height = builder.height;
        this.side = builder.side == null ? StampSide.RIGHT : builder.side;
        this.margin = Math.max(0, builder.margin);
        this.clipNumber = Math.max(0, builder.clipNumber);
        this.image = builder.image == null ? new byte[0] : Arrays.copyOf(builder.image, builder.image.length);
        this.ofdSeal = builder.ofdSeal == null ? new byte[0] : Arrays.copyOf(builder.ofdSeal, builder.ofdSeal.length);
    }

    public StampType type() {
        return type;
    }

    public int page() {
        return page;
    }

    public double x() {
        return x;
    }

    public double y() {
        return y;
    }

    public double width() {
        return width;
    }

    public double height() {
        return height;
    }

    public StampSide side() {
        return side;
    }

    public double margin() {
        return margin;
    }

    public int clipNumber() {
        return clipNumber;
    }

    public byte[] image() {
        return Arrays.copyOf(image, image.length);
    }

    public byte[] ofdSeal() {
        return Arrays.copyOf(ofdSeal, ofdSeal.length);
    }

    public boolean hasImage() {
        return image.length > 0;
    }

    public boolean hasOfdSeal() {
        return ofdSeal.length > 0;
    }

    public static Builder normal(int page, double x, double y, double width, double height) {
        return new Builder(StampType.NORMAL)
                .page(page)
                .position(x, y)
                .size(width, height);
    }

    public static Builder riding(StampSide side, double width, double height) {
        return new Builder(StampType.RIDING)
                .side(side)
                .size(width, height);
    }

    @LocalCapabilityContract
    public static final class Builder {

        private final StampType type;
        private int page = 1;
        private double x;
        private double y;
        private double width;
        private double height;
        private StampSide side = StampSide.RIGHT;
        private double margin;
        private int clipNumber;
        private byte[] image;
        private byte[] ofdSeal;

        private Builder(StampType type) {
            this.type = type;
        }

        public Builder page(int page) {
            this.page = page;
            return this;
        }

        public Builder position(double x, double y) {
            this.x = x;
            this.y = y;
            return this;
        }

        public Builder size(double width, double height) {
            this.width = width;
            this.height = height;
            return this;
        }

        public Builder side(StampSide side) {
            this.side = side;
            return this;
        }

        public Builder margin(double margin) {
            this.margin = margin;
            return this;
        }

        public Builder clipNumber(int clipNumber) {
            this.clipNumber = clipNumber;
            return this;
        }

        public Builder image(byte[] image) {
            this.image = image == null ? null : Arrays.copyOf(image, image.length);
            return this;
        }

        public Builder ofdSeal(byte[] ofdSeal) {
            this.ofdSeal = ofdSeal == null ? null : Arrays.copyOf(ofdSeal, ofdSeal.length);
            return this;
        }

        public DocumentStampCommand build() {
            return new DocumentStampCommand(this);
        }
    }
}
