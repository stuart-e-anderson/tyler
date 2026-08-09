class LogScrollbar extends java.awt.Scrollbar
{
    double min, max;
    public LogScrollbar(int orientation, int vis, int incrs, double val, double min, double max)
    {
        super(orientation,
              0,
              vis, 0, incrs+vis);
        this.min = min;
        this.max = max;
        setValueD(val);
    }
    public int getValue() // illegal to call
    {
        // XXX this gets called internally on Windows, so don't print this scary message.
        //System.out.println("WARNING: getValue() called on a log Scrollbar, did you mean getValueD()?");
        return super.getValue();
    }
    public double getValueD()
    {
        // XXXhatch in 1.0, the max / getVisible semantics seem different-- chedck this out! the max doesn't end up being 10000, but 15848.9
        double t = (double)(super.getValue() - super.getMinimum())
                 / (double)(super.getMaximum()-super.getVisible() - super.getMinimum());
        double dval = min * Math.pow(max/min, t);
        //System.out.println("getting: ival="+super.getValue()+" -> dval="+dval);
        return dval;
    }
    public void setValueD(double dval)
    {
        double t = Math.log(dval/min) / Math.log(max/min);
        int ival = (int)Math.round(super.getMinimum() + (super.getMaximum()-super.getVisible()-super.getMinimum()) * t);
        //System.out.println("setting: dval="+dval+" -> ival="+ival);
        setValue(ival);
    }
} // class LogScrollbar
