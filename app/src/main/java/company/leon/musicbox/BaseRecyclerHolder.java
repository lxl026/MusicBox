package company.leon.musicbox;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by Leon on 2017/11/20.
 */

public class BaseRecyclerHolder extends RecyclerView.ViewHolder {

    private SparseArray<View> views;
    private Context context;

    public BaseRecyclerHolder(Context context, View itemView, ViewGroup parent) {
        super(itemView);
        this.context = context;

        //指定一个初始为8
        views = new SparseArray<View>();
    }

    /**
     //     * 取得一个RecyclerHolder对象
     //     * @param context 上下文
     //     * @param itemView 子项
     //     * @return 返回一个RecyclerHolder对象
     */
    public static BaseRecyclerHolder getRecyclerHolder(Context context,ViewGroup parent,int layoutId){
        View itemView = LayoutInflater.from(context).inflate(layoutId,parent,false);
        BaseRecyclerHolder holder = new BaseRecyclerHolder(context,itemView ,parent);
        return holder;
    }

    public SparseArray<View> getViews(){
        return this.views;
    }

    /**
     * 通过view的id获取对应的控件，如果没有则加入views中
     * @param viewId 控件的id
     * @return 返回一个控件
     */
    @SuppressWarnings("unchecked")
    public <T extends View> T getView(int viewId){
        View view = views.get(viewId);
        if (view == null ){
            view = itemView.findViewById(viewId);
            views.put(viewId,view);
        }
        return (T) view;
    }

}

