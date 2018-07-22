package crinysoft.yellowduck;

public class ListViewItem {
    public static final int ITEM_TYPE_SEND = 0;
    public static final int ITEM_TYPE_RESV = 1;

    private int itemType;
    private String text;

    public ListViewItem(int itemType, String text) {
        this.itemType = itemType;
        this.text = text;
    }

    public int getItemType() {
        return itemType;
    }

    public String getText() {
        return text;
    }
}
