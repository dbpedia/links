for i in `find . -name "metadata.ttl"` 
do 
   # collect
   rapper -i turtle $i 2> /dev/null | grep uncompressedturtlefile  | cut -f2 -d '"' | xargs cat 
done
