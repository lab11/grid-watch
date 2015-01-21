set terminal postscript enhanced eps solid color font "Helvetica,30"
set size square
set output "high-low.eps"

set style line 1 lt 1  ps 2 pt 7 lw 5 lc rgb "#E66101"
set style line 2 lt 1  ps 2 pt 7 lw 1 lc rgb "#FDB863"

set border 0
set grid lw 1 lt 1 lc rgb "#bbbbbb"

set xrange[1392965120:1418661834]
set xtics("Date 1" 1392965120, "Date 2" 1418661834)
set xtics font "Helvetica,20"

set ylabel "Phone ID" font "Helvetica,30"
set yrange[0:10]
set ytics
set ytics font "Helvetica,20"

set bmargin
set rmargin 8.5

unset key
set datafile separator ","

plot 'high-low.csv' u 1:($3==0 ? $2 : $2+.5) ls 1 w l