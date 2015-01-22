set terminal postscript enhanced eps solid color font "Helvetica,30"
set size square
set output "high-low.eps"

set style line 1 lt 1 ps 1 pt 7 lw 2 lc rgb "#E66101"
set style line 2 lt 1  ps 2 pt 7 lw 1 lc rgb "#FDB863"

set border 3
set grid lw 1 lt 1 lc rgb "#bbbbbb"

set xrange[1398844462:1398963028]
set xtics("Date 1" 1398844462, "Date 2" 1398963028)
set xtics font "Helvetica,20"
set xtics nomirror

set ylabel "Phone ID" font "Helvetica,30"
set yrange[0:10]
set ytics 1, 1, 10
set ytics font "Helvetica,20"
set ytics nomirror

set bmargin
set rmargin 8.5

unset key
set datafile separator ","

plot 'digital_graph_sorted.csv' every ::1 u 1:($2==1 ? $2 + .5 * $3 : 1/0) ls 1 w l, \
	 'digital_graph_sorted.csv' every ::1 u 1:($2==2 ? $2 + .5 * $3 : 1/0) ls 1 w l, \
	 'digital_graph_sorted.csv' every ::1 u 1:($2==3 ? $2 + .5 * $3 : 1/0) ls 1 w l, \
	 'digital_graph_sorted.csv' every ::1 u 1:($2==4 ? $2 + .5 * $3 : 1/0) ls 1 w l, \
	 'digital_graph_sorted.csv' every ::1 u 1:($2==5 ? $2 + .5 * $3 : 1/0) ls 1 w l, \
	 'digital_graph_sorted.csv' every ::1 u 1:($2==6 ? $2 + .5 * $3 : 1/0) ls 1 w l, \
	 'digital_graph_sorted.csv' every ::1 u 1:($2==7 ? $2 + .5 * $3 : 1/0) ls 1 w l, \
	 'digital_graph_sorted.csv' every ::1 u 1:($2==8 ? $2 + .5 * $3 : 1/0) ls 1 w l, \
	 'digital_graph_sorted.csv' every ::1 u 1:($2==9 ? $2 + .5 * $3 : 1/0) ls 1 w l, \
	 'digital_graph_sorted.csv' every ::1 u 1:($2==10 ? $2 + .5 * $3 : 1/0) ls 1 w l

#plot 'digital_graph_sorted.csv' every ::1 u 1:( $2 == 1? $2 : 1/0) ls 1 w l