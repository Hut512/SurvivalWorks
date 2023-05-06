package de.survivalworkers.core.client.engine.graphics.vertex;

import de.survivalworkers.core.client.engine.graphics.rendering.Buffer;
import de.survivalworkers.core.client.engine.graphics.rendering.CommandBuffer;
import de.survivalworkers.core.client.engine.graphics.rendering.Image;
import de.survivalworkers.core.client.engine.graphics.rendering.ImageView;
import de.survivalworkers.core.client.vk.device.SWLogicalDevice;
import lombok.Getter;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

import java.io.Closeable;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.vulkan.VK10.*;

public class Texture implements Closeable {
    private final Image image;
    @Getter
    private final ImageView handle;
    private boolean recordedTransition;
    private String fileName;
    private int width;
    private int height;
    private int mipLvl;
    private Buffer stgBuffer;
    @Getter
    private boolean transparent;

    public Texture(SWLogicalDevice device, String fileName, int imageFormat) {
        recordedTransition = false;
        this.fileName = fileName;
        ByteBuffer buf;
        try(MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);
            IntBuffer channels = stack.mallocInt(1);

            buf = STBImage.stbi_load(fileName,w,h,channels,4);
            if (buf == null) {
                throw new RuntimeException("Could not load image:" + fileName + " due to " + STBImage.stbi_failure_reason());
            }
            width = w.get();
            height = h.get();
            mipLvl = (int) Math.floor(Math.log(Math.min(width,height)) / Math.log(2)) + 1;
            
            createStgBuffer(device,buf);
            Image.ImageData imageData = new Image.ImageData()
                    .width(width)
                    .height(height)
                    .usage(VK_IMAGE_USAGE_TRANSFER_DST_BIT | VK_IMAGE_USAGE_TRANSFER_SRC_BIT | VK_IMAGE_USAGE_SAMPLED_BIT)
                    .format(imageFormat)
                    .mipLevels(mipLvl);
            image = new Image(device,imageData);
            ImageView.ImageViewData viewData = new ImageView.ImageViewData()
                    .format(image.getFormat())
                    .aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                    .mipLevels(mipLvl);
            handle = new ImageView(device,image.getImage(),viewData);
            STBImage.stbi_image_free(buf);
        }
    }

    private void setTransparency(ByteBuffer bp){
        transparent = false;
        for (int i = 0; i < bp.capacity(); i += 4) {
            if((0xFF & bp.get(i + 3)) < 255){
                transparent = true;
                break;
            }
        }
    }

    private void createStgBuffer(SWLogicalDevice device, ByteBuffer buf) {
        stgBuffer = new Buffer(device,buf.remaining(),VK_BUFFER_USAGE_TRANSFER_SRC_BIT,VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT);
        long mappedMemory = stgBuffer.map();
        ByteBuffer buffer = MemoryUtil.memByteBuffer(mappedMemory, (int) stgBuffer.getRequestedSize());
        buffer.put(buf);
        buf.flip();
        stgBuffer.unMap();
    }

    public void close(){
        if(stgBuffer != null){
            stgBuffer.close();
            stgBuffer = null;
        }
        image.close();
        handle.close();
    }

    public String getFileName() {
        return fileName;
    }

    public void recordTextureTransition(CommandBuffer cmdBuf){
        if(stgBuffer == null && recordedTransition)return;
        recordedTransition = true;
        try(MemoryStack stack = MemoryStack.stackPush()) {
            recordImageTransition(stack,cmdBuf, VK_IMAGE_LAYOUT_UNDEFINED, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL);
            recordCopyBuffer(stack,cmdBuf,stgBuffer);
            recordGeneratedMipMaps(stack,cmdBuf);
        }
    }

    private void recordGeneratedMipMaps(MemoryStack stack, CommandBuffer cmdBuf) {
        VkImageSubresourceRange subRange = VkImageSubresourceRange.calloc(stack).aspectMask(VK_IMAGE_ASPECT_COLOR_BIT).baseArrayLayer(0).levelCount(1).layerCount(1);
        VkImageMemoryBarrier.Buffer barrier = VkImageMemoryBarrier.calloc(1,stack).sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER).image(image.getImage()).srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED).
                dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED).subresourceRange(subRange);
        int mipWidth = width;
        int mipHeight = height;
        for (int i = 0; i < mipLvl; i++) {
            subRange.baseMipLevel(i - 1);
            barrier.subresourceRange(subRange).oldLayout(VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL).newLayout(VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL).srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT).
                    dstAccessMask(VK_ACCESS_TRANSFER_READ_BIT);
            vkCmdPipelineBarrier(cmdBuf.getCmdBuf(),VK_PIPELINE_STAGE_TRANSFER_BIT,VK_PIPELINE_STAGE_TRANSFER_BIT,0,null,null,barrier);

            int j = i;
            VkOffset3D srcOffset0 = VkOffset3D.calloc(stack).x(0).y(0).z(0);
            VkOffset3D srcOffset1 = VkOffset3D.calloc(stack).x(mipWidth).y(mipHeight).z(1);
            VkOffset3D dstOffset0 = VkOffset3D.calloc(stack).x(0).y(0).z(0);
            VkOffset3D dstOffset1 = VkOffset3D.calloc(stack).x(mipWidth > 1 ? mipWidth / 2 : 1).y(mipHeight > 1 ? mipHeight / 2 : 1).z(1);
            VkImageBlit.Buffer blit = VkImageBlit.calloc(1,stack).srcOffsets(0,srcOffset0).srcOffsets(1,srcOffset1).srcSubresource(it -> it.aspectMask(VK_IMAGE_ASPECT_COLOR_BIT).mipLevel(j - 1).
                    baseArrayLayer(0).layerCount(1)).dstOffsets(0,dstOffset0).dstOffsets(1,dstOffset1).dstSubresource(it -> it.aspectMask(VK_IMAGE_ASPECT_COLOR_BIT).mipLevel(j).baseArrayLayer(0).
                    layerCount(1));

            vkCmdBlitImage(cmdBuf.getCmdBuf(), image.getImage(),VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL, image.getImage(),VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,blit,VK_FILTER_LINEAR);

            barrier.oldLayout(VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL).newLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL).srcAccessMask(VK_ACCESS_TRANSFER_READ_BIT).dstAccessMask(VK_ACCESS_SHADER_READ_BIT);
            vkCmdPipelineBarrier(cmdBuf.getCmdBuf(),VK_PIPELINE_STAGE_TRANSFER_BIT,VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT,0,null,null,barrier);
            if(mipWidth > 1)mipWidth /= 2;
            if(mipHeight > 1)mipHeight /= 2;
            barrier.subresourceRange(it -> it.baseMipLevel(mipLvl - 1)).oldLayout(VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL).newLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL).srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT).
                    dstAccessMask(VK_ACCESS_SHADER_READ_BIT);
            vkCmdPipelineBarrier(cmdBuf.getCmdBuf(),VK_PIPELINE_STAGE_TRANSFER_BIT,VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT,0,null,null,barrier);
        }
    }

    private void recordCopyBuffer(MemoryStack stack, CommandBuffer cmdBuf, Buffer stgBuffer) {
        VkBufferImageCopy.Buffer region = VkBufferImageCopy.calloc(1,stack).bufferOffset(0).bufferRowLength(0).bufferImageHeight(0).imageSubresource(it -> it.aspectMask(VK_IMAGE_ASPECT_COLOR_BIT).
                mipLevel(0).baseArrayLayer(0).layerCount(1)).imageOffset(it -> it.x(0).y(0).z(0)).imageExtent(it -> it.width(width).height(height).depth(1));

        vkCmdCopyBufferToImage(cmdBuf.getCmdBuf(),stgBuffer.getBuffer(),image.getImage(),VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,region);
    }

    private void recordImageTransition(MemoryStack stack, CommandBuffer cmdBuf, int old, int newLayout) {
        VkImageMemoryBarrier.Buffer barrier = VkImageMemoryBarrier.calloc(1,stack).sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER).oldLayout(old).newLayout(newLayout).srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED).
                dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED).image(image.getImage()).subresourceRange(it -> it.aspectMask(VK_IMAGE_ASPECT_COLOR_BIT).baseMipLevel(0).levelCount(mipLvl).baseArrayLayer(1).
                        levelCount(1));

        int srcStage;
        int srcMask;
        int dstStage;
        int dstMask;
        if(old == VK_IMAGE_LAYOUT_UNDEFINED && newLayout == VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL){
            srcStage = VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT;
            srcMask = 0;
            dstStage = VK_PIPELINE_STAGE_TRANSFER_BIT;
            dstMask = VK_ACCESS_TRANSFER_READ_BIT;
        }else if(old == VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL && newLayout == VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL){
            srcStage = VK_PIPELINE_STAGE_TRANSFER_BIT;
            srcMask = VK_ACCESS_TRANSFER_WRITE_BIT;
            dstStage = VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT;
            dstMask = VK_ACCESS_SHADER_READ_BIT;
        }else throw new RuntimeException("Unsupported layout transition");
        barrier.srcAccessMask(srcMask);
        barrier.dstAccessMask(dstMask);
        vkCmdPipelineBarrier(cmdBuf.getCmdBuf(),srcStage,dstStage,0,null,null,barrier);
    }
}
