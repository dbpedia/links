# ignore rapper error "ASCII character",
# see http://bugs.librdf.org/mantis/view.php?id=545

echo "" > /tmp/gen
for i in `find . -name "*.nt"`
 do  rapper -i ntriples $i | cut -f2 -d '>' | sed 's/<//' >> /tmp/gen
done

sort  /tmp/gen | uniq -c | sort -rn > predicate-count.csv
