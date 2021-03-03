package com.easydicm.scputil;

import org.springframework.util.Assert;

import java.io.Closeable;
import java.nio.ByteBuffer;

/***
 * 处理大文件读写问题,解决MappedByteBuffer 不能动态扩容的问题
 */
public class MemPool implements Closeable {


    static class MemNode {

        final long segMapPostion;
        final long segMapSize;
        final long segIndex;

        public long getSegMapPostion() {
            return segMapPostion;
        }

        public long getSegMapSize() {
            return segMapSize;
        }

        public long getSegIndex() {
            return segIndex;
        }

        public MemNode getPreNode() {
            return preNode;
        }

        public MemNode getNxtPode() {
            return nxtPode;
        }

        public void setPreNode(MemNode preNode) {
            this.preNode = preNode;
        }

        public void setNxtPode(MemNode nxtPode) {
            this.nxtPode = nxtPode;
        }

        MemNode preNode;
        MemNode nxtPode;

        public ByteBuffer getDataBuffer() {
            return dataBuffer;
        }

        final ByteBuffer dataBuffer;

        public MemNode(long segIndex, long segPostion, long size) {
            this.segIndex = segIndex;
            segMapPostion = segPostion;
            segMapSize = size;
            preNode = null;
            nxtPode = null;
            Assert.isTrue(size < Integer.MAX_VALUE, "容量超过最大范围");
            dataBuffer = ByteBuffer.allocateDirect((int) size);

            Assert.isTrue(dataBuffer.capacity() == size, "Cap分配成功");
            Assert.isTrue(dataBuffer.limit() == size, "limit分配成功");
            Assert.isTrue(dataBuffer.position() == 0L, "pos分配成功");
        }

        public void close() {
            dataBuffer.clear();
        }


    }

    /***
     * 每个缓存节为 512M
     */

    // 512M;
    static final long MAP_SEG = 10L;

    public MemNode[] getNodes() {
        return nodes;
    }

    public long getMapSize() {
        return mapSize;
    }

    final long mapSize;

    final int nodeSize;
    final MemNode[] nodes;

    long cpos;


    MemNode cNode;

    /***
     * 内存映射池缓冲,支持读写
     * @param size 内存分配大小
     */
    public MemPool(long size)  {

        mapSize = size;

        long ga = size / MAP_SEG;
        long gb = size % MAP_SEG;
        int sz = (int) ga;
        if (gb > 0) {
            sz += 1;
        }
        nodes = new MemNode[sz];
        for (int i = 0; i < sz; i++) {
            nodes[i] = new MemNode(i, i  * MAP_SEG, i == sz - 1 ? gb : MAP_SEG);
            if (i > 0) {
                nodes[i].setPreNode(nodes[i - 1]);
            }
        }
        for (int i = 0; i < sz - 1; i++) {
            nodes[i].setNxtPode(nodes[i + 1]);
        }
        cpos = 0L;
        cNode = nodes[0];
        nodeSize = nodes.length;

    }

    @Override
    public void close()  {
        for (MemNode mn : nodes) {
            mn.close();
        }
    }

    public long getPosition() {
        return cNode.getSegMapPostion() + cNode.getDataBuffer().position();
    }


    public MemNode currentNode() {
        return cNode;
    }


    /***
     * 跳到指定位置
     * @param newPosition  跳转到指定位置
     * @return
     *    newPosition <=0    跳到0
     *    newPosition >= size   跳到最后
     *    跳到指定位置
     */
    public long seekPosiont(long newPosition) {
        if (newPosition <= 0) {
            cNode = nodes[0];
            cNode.getDataBuffer().position(0);
            cpos = 0 ;
        } else if (newPosition >= mapSize) {
            cNode = nodes[nodeSize - 1];
            cNode.getDataBuffer().position((int) cNode.getSegMapSize());
            cpos = mapSize;
        } else {
            long seg = newPosition / MAP_SEG;
            long lft = newPosition % MAP_SEG;
            cNode = nodes[(int) seg];
            cNode.getDataBuffer().position((int) lft);
            cpos = newPosition;
        }
        MemNode  afterNode=cNode.getNxtPode();
        while( afterNode != null ){
            afterNode.getDataBuffer().position(0);
            afterNode = afterNode.getNxtPode();
        }
        MemNode  preNode=cNode.getPreNode();
        while( preNode != null ){
            preNode.getDataBuffer().position( (int) preNode.getSegMapSize());
            preNode = preNode.getPreNode();
        }
        return cpos;


    }



    long    markPos = -1L;

    /***
     * 标记当前位置
     */
    public void mark() {
        markPos = getPosition();

    }

    /***
     * 回复到标记前的位置
     */
    public void reset() {
        if (markPos  == -1)
            return;
        seekPosiont(markPos);
    }

    public void write(byte[] data, int offset, int size) {
        long mlen =  size + cpos;
        if (mlen > mapSize) {
            return;
        }
        //--后面的指针都跳到  = 0

        int space = cNode.getDataBuffer().capacity() - cNode.getDataBuffer().position();
        if (size < space) {
            cNode.getDataBuffer().put(data, offset, size);
            cpos = getPosition();
        } else if (size == space) {
            cNode.getDataBuffer().put(data, offset, size);
            if( cNode.getNxtPode() != null ){
                cNode = cNode.getNxtPode();
            }
            cpos = getPosition();

        } else {
            cNode.getDataBuffer().put(data, offset, space);
            cNode = cNode.nxtPode;
            cpos = getPosition();
            int voffset = offset + space;
            int leftSize = size - space;
            write(data, voffset,  leftSize);
        }


    }

    /***
     * 写入数组,并返回写入的字节数
     * @param data 数组
     */
    public void write(byte[] data) {
        write(data,0,data.length);
    }

    public  void read(byte[] data){
        read(data,0, data.length);
    }

    public  void read(byte[] data,int offset,int size){
        long mlen =  size + cpos;
        if (mlen > mapSize) {
            return;
        }
        MemNode  afterNode=cNode.getNxtPode();
        while( afterNode != null ){
            afterNode.getDataBuffer().position(0);
            afterNode = afterNode.getNxtPode();
        }
        int space = cNode.getDataBuffer().capacity() - cNode.getDataBuffer().position();
        if (size < space) {
            //---就在当前节点读取
            cNode.getDataBuffer().get(data,offset, size);
            cpos = getPosition();
        } else if (size == space) {
            cNode.getDataBuffer().get(data, offset, size);
            if( cNode.getNxtPode() != null ){
                cNode = cNode.getNxtPode();
            }
            cpos = getPosition();
        } else {
            cNode.getDataBuffer().get(data, offset, space);
            cNode = cNode.nxtPode;
            cpos = getPosition();
            int voffset = offset + space;
            int leftSize = size - space;
            read(data, voffset,  leftSize);
        }


    }
}
