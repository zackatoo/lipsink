package com.zackatoo.lipsink;

import javafx.stage.Stage;

public abstract class LipSinkSubControllers
{
    protected Stage parentStage;
    protected Stage thisStage;
    private boolean showParent;
    protected LipSinkController controller;

    public void setParentStage(Stage s)
    {
        parentStage = s;
    }

    public void setThisStage(Stage s, boolean showParent)
    {
        thisStage = s;
        this.showParent = showParent;
        thisStage.setOnCloseRequest(event -> shutdown(true));
    }

    public void setParentController(LipSinkController c)
    {
        controller = c;
    }

    public void shutdown(boolean performCloseout)
    {
        if (performCloseout) closeout();
        if (showParent) parentStage.show();
        thisStage.close();
    }

    public void startup()
    {
    }

    protected void closeout()
    {
    }
}
